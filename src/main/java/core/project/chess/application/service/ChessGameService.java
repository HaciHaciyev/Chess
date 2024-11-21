package core.project.chess.application.service;

import core.project.chess.application.dto.gamesession.*;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.cache.PartnershipGameInvitationsService;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import core.project.chess.infrastructure.utilities.json.JSONUtilities;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.infrastructure.utilities.web.WSUtilities.*;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final PartnershipGameInvitationsService partnershipGameCacheService;

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> sessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Pair<ChessGame, HashSet<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, Triple<Session, UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    public void onOpen(Session session, Username username) {
        CompletableFuture.runAsync(() -> {
            Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
            if (!result.success()) {
                closeSession(session, Message.error("This account is do not founded."));
                return;
            }

            sessions.put(username, Pair.of(session, result.value()));
            partnershipGameCacheService
                    .getAll(username.username())
                    .forEach((key, value) -> {
                        String message = Message.invitation(key, value);
                        sendMessage(session, message);
                    });
        });
    }

    public void onMessage(Session session, Username username, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, Message.error("Message can't be null or blank."));
            return;
        }

        final Result<Message, Throwable> msg = JSONUtilities.readAsMessage(message);
        if (!msg.success()) {
            sendMessage(session, Message.error(msg.throwable().getMessage()));
            return;
        }

        final boolean isGameInitialization = msg.value().type().equals(MessageType.GAME_INIT);
        if (isGameInitialization) {
            initializeGameSession(session, username, message);
            return;
        }

        final String gameID = msg.value().gameID();
        if (Objects.isNull(gameID) || gameID.isBlank()) {
            sendMessage(session, Message.error("Game id is required."));
            return;
        }

        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            sendMessage(session, Message.error("Game id is required."));
            return;
        }

        if (gameIdObj instanceof List<?> ls && !ls.contains(gameID)) {
            sendMessage(session, Message.error("This game id is do not exists."));
            return;
        }

        final Pair<ChessGame, HashSet<Session>> gamePlusSessions = gameSessions.get(UUID.fromString(gameID));
        if (Objects.isNull(gamePlusSessions)) {
            sendMessage(session, Message.error("This game session does not exist."));
            return;
        }

        CompletableFuture.runAsync(
                () -> handleWebSocketMessage(session, username.username(), msg.value(), gamePlusSessions)
        );
    }

    private void handleWebSocketMessage(final Session session, final String username, final Message message,
                                        final Pair<ChessGame, HashSet<Session>> gameSessions) {
        switch (message.type()) {
            case MOVE -> this.move(message, Pair.of(username, session), gameSessions);
            case MESSAGE -> this.chat(message, Pair.of(username, session), gameSessions);
            case RETURN_MOVE -> this.returnOfMovement(Pair.of(username, session), gameSessions);
            case RESIGNATION -> this.resignation(Pair.of(username, session), gameSessions);
            case TREE_FOLD -> this.threeFold(Pair.of(username, session), gameSessions);
            case AGREEMENT -> this.agreement(Pair.of(username, session), gameSessions);
            default -> sendMessage(session, Message.error("Invalid message type."));
        }
    }

    private void initializeGameSession(Session session, Username username, String message) {
        CompletableFuture.runAsync(() -> {
            final Result<GameInit, Throwable> parameters = JSONUtilities.gameInit(message);
            if (!parameters.success()) {
                sendMessage(session, Message.error("Invalid game initialization parameters"));
                return;
            }

            final boolean connectToExistedGame = Objects.nonNull(parameters.value().gameId());
            if (connectToExistedGame) {
                joinExistingGameSession(session, parameters.value().gameId());
                return;
            }

            final boolean partnershipGame = Objects.nonNull(parameters.value().nameOfPartner());
            if (partnershipGame) {
                handlePartnershipGameRequest(session, username, parameters.value());
                return;
            }

            startNewGame(session, username, parameters.value());
        });
    }

    private void joinExistingGameSession(Session session, UUID gameId) {
        final Pair<ChessGame, HashSet<Session>> gameData = gameSessions.get(gameId);
        gameData.getSecond().add(session);

        sendGameStartNotifications(session, gameData.getFirst());
    }

    private void startNewGame(Session session, Username username, GameInit parameters) {
        final GameParameters gameParameters = new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());
        final UserAccount firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();

        sendMessage(session, Message.info("Finding opponent..."));

        final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = locateOpponentForGame(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            waitingForTheGame.put(username, Triple.of(session, firstPlayer, gameParameters));

            String message = Message
                    .userInfo("Trying to find an opponent for you %s.".formatted(username.username()));

            sendMessage(session, message);
            return;
        }

        final Triple<Session, UserAccount, GameParameters> opponentData = potentialOpponent.orElseThrow();
        final Session secondSession = opponentData.getFirst();
        final UserAccount secondPlayer = opponentData.getSecond();
        final GameParameters secondGameParameters = opponentData.getThird();

        waitingForTheGame.remove(secondPlayer.getUsername());

        startStandardChessGame(
                Triple.of(session, firstPlayer, gameParameters),
                Triple.of(secondSession, secondPlayer, secondGameParameters)
        );
    }

    private StatusPair<Triple<Session, UserAccount, GameParameters>> locateOpponentForGame(final UserAccount firstPlayer,
                                                                                           final GameParameters gameParameters) {
        for (var entry : waitingForTheGame.entrySet()) {
            final UserAccount potentialOpponent = entry.getValue().getSecond();
            final GameParameters gameParametersOfPotentialOpponent = entry.getValue().getThird();

            final boolean sameUser = potentialOpponent.getId().equals(firstPlayer.getId());
            if (sameUser) {
                continue;
            }

            final boolean isOpponent = this.validateOpponentEligibility(firstPlayer, gameParameters, potentialOpponent, gameParametersOfPotentialOpponent);
            if (isOpponent) {
                return StatusPair.ofTrue(entry.getValue());
            }
        }

        return StatusPair.ofFalse();
    }

    private boolean validateOpponentEligibility(final UserAccount player, final GameParameters gameParameters,
                                                final UserAccount opponent, final GameParameters opponentGameParameters) {
        final boolean sameTimeControlling = gameParameters.timeControllingTYPE().equals(opponentGameParameters.timeControllingTYPE());
        if (!sameTimeControlling) {
            return false;
        }

        final boolean validRatingDiff = Math.abs(player.getRating().rating() - opponent.getRating().rating()) <= 1500;
        if (!validRatingDiff) {
            return false;
        }

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) {
            return true;
        }

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }

    private void handlePartnershipGameRequest(Session session, Username addresser, GameInit parameters) {
        Objects.requireNonNull(parameters.nameOfPartner());
        if (!outboundUserRepository.isUsernameExists(parameters.nameOfPartner())) {
            sendMessage(session, Message.error("User %s do not exists.".formatted(parameters.nameOfPartner().username())));
            return;
        }

        final UserAccount addresserAccount = outboundUserRepository.findByUsername(addresser).orElseThrow();

        final UserAccount addresseeAccount = Objects.requireNonNullElseGet(
                sessions.get(parameters.nameOfPartner()).getSecond(), () -> outboundUserRepository.findByUsername(parameters.nameOfPartner()).orElseThrow()
        );
        final String addressee = addresseeAccount.getUsername().username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        if (!isHavePartnership) {
            sendMessage(session, Message.error("You can`t invite someone who`s have not partnership with you."));
            return;
        }

        final var gameParametersOfAddresser = new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());
        partnershipGameCacheService.put(addressee, addresser.username(), gameParametersOfAddresser);

        final StatusPair<GameParameters> isPartnershipGameAgreed = checkPartnershipAgreement(addressee, addresser.username());
        if (isPartnershipGameAgreed.status()) {
            var addresserSession = sessions.get(addresser).getFirst();
            var addresseeSession = sessions.get(addresseeAccount.getUsername()).getFirst();

            startPartnershipGame(
                    Triple.of(addresserSession, addresserAccount, gameParametersOfAddresser),
                    Triple.of(addresseeSession, addresseeAccount, gameParametersOfAddresser)
            );
        }
    }

    private StatusPair<GameParameters> checkPartnershipAgreement(String addressee, String addresser) {
        final Map<String, GameParameters> requests = partnershipGameCacheService.getAll(addresser);
        if (requests.containsKey(addressee)) {
            return StatusPair.ofTrue(requests.get(addressee));
        }

        return StatusPair.ofFalse();
    }

    private void startPartnershipGame(Triple<Session, UserAccount, GameParameters> addresserData,
                                      Triple<Session, UserAccount, GameParameters> addresseeData) {
        Session addresser = addresserData.getFirst();
        UserAccount addresserAccount = addresserData.getSecond();
        GameParameters gameParametersOfAddresser = addresserData.getThird();

        Session addressee = addresseeData.getFirst();
        UserAccount addresseeAccount = addresseeData.getSecond();
        GameParameters gameParametersOfAddressee = addresseeData.getThird();

        Objects.requireNonNull(addresser, "Addresser session must not be null");
        if (Objects.isNull(addressee)) {
            String message = Message.error("The request timed out. Can't initialize a game.");
            sendMessage(addresser, message);
            return;
        }

        ChessGame chessGame = createChessGameInstance(addresserAccount, gameParametersOfAddresser, addresseeAccount, gameParametersOfAddressee);
        registerGameAndNotifyPlayers(chessGame, addresser, addressee);

        partnershipGameCacheService.delete(addresserAccount.getUsername().username(), addresseeAccount.getUsername().username());
        partnershipGameCacheService.delete(addresseeAccount.getUsername().username(), addresserAccount.getUsername().username());

        inboundChessRepository.completelySaveStartedChessGame(chessGame);

        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    private void startStandardChessGame(Triple<Session, UserAccount, GameParameters> firstPlayerData,
                                        Triple<Session, UserAccount, GameParameters> secondPlayerData) {
        Session firstSession = firstPlayerData.getFirst();
        UserAccount firstPlayer = firstPlayerData.getSecond();
        GameParameters firstGameParameters = firstPlayerData.getThird();

        Session secondSession = secondPlayerData.getFirst();
        UserAccount secondPlayer = secondPlayerData.getSecond();
        GameParameters secondGameParameters = secondPlayerData.getThird();

        ChessGame chessGame = createChessGameInstance(firstPlayer, firstGameParameters, secondPlayer, secondGameParameters);
        registerGameAndNotifyPlayers(chessGame, firstSession, secondSession);

        inboundChessRepository.completelySaveStartedChessGame(chessGame);

        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    private ChessGame createChessGameInstance(final UserAccount firstPlayer, final GameParameters gameParameters,
                                              final UserAccount secondPlayer, final GameParameters secondGameParameters) {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());
        final ChessGame.TimeControllingTYPE timeControlling = gameParameters.timeControllingTYPE();
        final boolean firstPlayerIsWhite = Objects.nonNull(gameParameters.color()) && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = Objects.nonNull(secondGameParameters.color()) && secondGameParameters.color().equals(Color.BLACK);

        if (firstPlayerIsWhite && secondPlayerIsBlack) {
            return ChessGame.of(UUID.randomUUID(), chessBoard, firstPlayer, secondPlayer, SessionEvents.defaultEvents(), timeControlling);
        }

        return ChessGame.of(UUID.randomUUID(), chessBoard, secondPlayer, firstPlayer, SessionEvents.defaultEvents(), timeControlling);
    }

    private void registerGameAndNotifyPlayers(ChessGame chessGame, Session firstSession, Session secondSession) {
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>(Arrays.asList(firstSession, secondSession))));

        sendGameStartNotifications(firstSession, chessGame);
        sendGameStartNotifications(secondSession, chessGame);

        String gameId = chessGame.getChessGameId().toString();
        updateSessionGameIds(firstSession, gameId);
        updateSessionGameIds(secondSession, gameId);
    }

    private void updateSessionGameIds(Session session, String gameId) {
        //TODO unchecked cast
        final List<String> gameIds = (List<String>) session.getUserProperties().computeIfAbsent("game-id", key -> new ArrayList<>());
        if (!gameIds.contains(gameId)) {
            gameIds.add(gameId);
        }
    }

    //TODO make build() to return JSON string
    private void sendGameStartNotifications(Session session, ChessGame chessGame) {
        final String overviewMessage = Message.builder(MessageType.GAME_START_INFO)
                .gameID(chessGame.getChessGameId().toString())
                .whitePlayerUsername(chessGame.getPlayerForWhite().getUsername())
                .blackPlayerUsername(chessGame.getPlayerForBlack().getUsername())
                .whitePlayerRating(chessGame.getPlayerForWhiteRating().rating())
                .blackPlayerRating(chessGame.getPlayerForBlackRating().rating())
                .time(chessGame.getTimeControllingTYPE())
                .build()
                .asJSON();

        final String message = Message.builder(MessageType.FEN_PGN)
                .gameID(chessGame.getChessGameId().toString())
                .FEN(chessGame.getChessBoard().actualRepresentationOfChessBoard())
                .PGN(chessGame.getChessBoard().pgn())
                .build().asJSON();

        sendMessage(session, overviewMessage);
        sendMessage(session, message);
    }

    private void move(Message move, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameSessions) {
        final String username = usernameSession.getFirst();
        final ChessGame cg = gameSessions.getFirst();

        try {
            cg.makeMovement(
                    username, move.from(), move.to(),
                    Objects.isNull(move.inCaseOfPromotion()) ? null : AlgebraicNotation.fromSymbol(move.inCaseOfPromotion())
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Invalid chess movement.")
                    .gameID(cg.getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameSession.getSecond(), message);
            return;
        }

        final String message = Message.builder(MessageType.FEN_PGN)
                .gameID(cg.getChessGameId().toString())
                .FEN(cg.getChessBoard().actualRepresentationOfChessBoard())
                .PGN(cg.getChessBoard().pgn())
                .build()
                .asJSON();

        for (Session currentSession : gameSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void chat(Message message, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameSession.getFirst();

        try {
            ChatMessage chatMsg = new ChatMessage(message.message());
            gameAndSessions.getFirst().addChatMessage(username, chatMsg);

            final String msg = Message.builder(MessageType.MESSAGE)
                    .message(chatMsg.message())
                    .build()
                    .asJSON();

            gameAndSessions.getSecond().forEach(session -> sendMessage(session, msg));
        } catch (IllegalArgumentException | NullPointerException e) {
            String errorMessage = Message.builder(MessageType.ERROR)
                    .message("Invalid message.")
                    .gameID(gameAndSessions.getFirst().getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameSession.getSecond(), errorMessage);
        }
    }

    public void returnOfMovement(Pair<String, Session> usernameAndSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame cg = gameAndSessions.getFirst();

        try {
            cg.returnMovement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Can`t return a move.")
                    .gameID(cg.getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameAndSession.getSecond(), message);
            return;
        }

        if (!cg.isLastMoveWasUndo()) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Player {%s} requested for move returning.".formatted(username))
                    .gameID(cg.getChessGameId().toString())
                    .build()
                    .asJSON();

            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message));
            return;
        }

        final String message = Message.builder(MessageType.FEN_PGN)
                .gameID(cg.getChessGameId().toString())
                .FEN(cg.getChessBoard().actualRepresentationOfChessBoard())
                .PGN(cg.getChessBoard().pgn())
                .build()
                .asJSON();

        gameAndSessions.getSecond().forEach(currentSession -> sendMessage(currentSession, message));
    }

    public void resignation(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.resignation(username);

            final String message = Message.builder(MessageType.GAME_ENDED)
                    .gameID(chessGame.getChessGameId().toString())
                    .message("Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                    .build()
                    .asJSON();

            gameAndSessions.getSecond().forEach(currentSession -> sendMessage(currentSession, message));
        } catch (IllegalArgumentException e) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Not a player.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameAndSession.getSecond(), message);
        }
    }

    public void threeFold(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Can`t end game by ThreeFold")
                    .gameID(chessGame.getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameAndSession.getSecond(), message);
            return;
        }

        final String message = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.getChessGameId().toString())
                .message("Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                .build()
                .asJSON();

        gameAndSessions.getSecond().forEach(currentSession -> sendMessage(currentSession, message));
    }

    public void agreement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.agreement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = Message.builder(MessageType.ERROR)
                    .message("Not a player. Illegal access.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build()
                    .asJSON();

            sendMessage(usernameAndSession.getSecond(), message);
            return;
        }

        if (!chessGame.isAgreementAvailable()) {
            final String message = Message.builder(MessageType.AGREEMENT)
                    .gameID(chessGame.getChessGameId().toString())
                    .message("Player {%s} requested for agreement.".formatted(username))
                    .build()
                    .asJSON();

            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message));
            return;
        }

        final String message = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.getChessGameId().toString())
                .message("Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                .build()
                .asJSON();

        gameAndSessions.getSecond().forEach(currentSession -> sendMessage(currentSession, message));
    }

    public void onClose(Session session) {
        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            return;
        }

        for (Object gameId : (List<?>) gameIdObj) {
            final UUID gameUuid = UUID.fromString((String) gameId);

            final boolean isGameSessionExists = gameSessions.containsKey(gameUuid);
            if (!isGameSessionExists) {
                sendMessage(session, Message.error("Game session with id {%s} does not exist".formatted(gameId)));
                return;
            }

            final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameUuid);

            final ChessGame chessGame = pair.getFirst();
            if (chessGame.gameResult().isEmpty()) {
                return;
            }

            final Set<Session> sessionHashSet = pair.getSecond();
            final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().orElseThrow().toString());
            closeSession(session, Message.info(messageInCaseOfGameEnding));

            sessionHashSet.remove(session);
            if (sessionHashSet.isEmpty()) {
                gameSessions.remove(gameUuid);
            }
        }
    }

    private void gameOverOperationsExecutor(final ChessGame chessGame) {
        Log.info("Game over operations executing.");
        if (outboundChessRepository.isChessHistoryPresent(chessGame.getChessBoard().getChessBoardId())) {
            Log.infof("History of game %s is already present", chessGame.getChessGameId());
            return;
        }

        Log.infof("Saving finished game %s and changing ratings", chessGame.getChessGameId());
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);
        inboundUserRepository.updateOfRating(chessGame.getPlayerForWhite());
        inboundUserRepository.updateOfRating(chessGame.getPlayerForBlack());
    }

    private class ChessGameSpectator implements Runnable {
        private final ChessGame game;
        private final AtomicBoolean isRunning;
        private final ExecutorService executor;

        public ChessGameSpectator(ChessGame game) {
            this.game = game;
            this.isRunning = new AtomicBoolean(false);
            this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Spectator Thread"));
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                game.gameResult().ifPresent(gameResult -> {
                    Log.infof("Game is over by result {%s}", gameResult);
                    Log.debugf("Removing game {%s}", game.getChessGameId());
                    var gameAndSessions = gameSessions.remove(game.getChessGameId());

                    CompletableFuture.runAsync(() -> gameOverOperationsExecutor(game));

                    for (Session session : gameAndSessions.getSecond()) {
                        Log.infof("Sending game result {%s} to session {%s}", gameResult, session.getId());
                        sendMessage(session, "Game is over by result {%s}".formatted(gameResult));
                    }

                    isRunning.set(false);
                });
            }
            Log.info("Spectator shutting down");
        }

        public void start() {
            if (isRunning.get()) {
                Log.debug("Spectator is already running");
            }

            Log.info("Starting spectator");
            isRunning.set(true);
            executor.submit(this);
        }
    }
}
