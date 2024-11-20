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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.infrastructure.utilities.web.WSUtilities.*;

@Slf4j
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

    public void handleOnOpen(Session session, Username username) {
        CompletableFuture.runAsync(() -> {
            Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
            if (!result.success()) {
                closeSession(session, JSONUtilities.write(Message.error("This account is do not founded.")).orElseThrow());
                return;
            }

            sessions.put(username, Pair.of(session, result.value()));
            partnershipGameCacheService
                    .getAll(username.username())
                    .forEach(
                            (key, value) -> sendMessage(session, JSONUtilities.write(Message.invitation(key, value)).orElseThrow())
                    );
        });
    }

    public void handleOnMessage(Session session, Username username, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, JSONUtilities.write(Message.error("Message can't be null or blank.")).orElseThrow());
            return;
        }

        final Result<Message, Throwable> msg = JSONUtilities.readAsMessage(message);
        if (!msg.success()) {
            sendMessage(session, JSONUtilities.write(Message.error(msg.throwable().getMessage())).orElseThrow());
            return;
        }

        final boolean isGameInitialization = msg.value().type().equals(MessageType.GAME_INIT);
        if (isGameInitialization) {
            gameInitialization(session, username, message);
            return;
        }

        final String gameID = msg.value().gameID();
        if (Objects.isNull(gameID) || gameID.isBlank()) {
            sendMessage(session, JSONUtilities.write(Message.error("Game id is required.")).orElseThrow());
            return;
        }

        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            sendMessage(session, JSONUtilities.write(Message.error("Game id is required.")).orElseThrow());
            return;
        }

        if (gameIdObj instanceof List<?> ls && !ls.contains(gameID)) {
            sendMessage(session, JSONUtilities.write(Message.error("This game id is do not exists.")).orElseThrow());
            return;
        }

        final Pair<ChessGame, HashSet<Session>> gamePlusSessions = gameSessions.get(UUID.fromString(gameID));
        if (Objects.isNull(gamePlusSessions)) {
            sendMessage(session, JSONUtilities.write(Message.error("This game session does not exist.")).orElseThrow());
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
            default -> sendMessage(session, JSONUtilities.write(Message.error("Invalid message type.")).orElseThrow());
        }
    }

    private void gameInitialization(Session session, Username username, String message) {
        CompletableFuture.runAsync(() -> {
            final Result<GameInit, Throwable> parameters = JSONUtilities.gameInit(message);
            if (!parameters.success()) {
                sendMessage(session, JSONUtilities.write(Message.error("Invalid game initialization parameters")).orElseThrow());
                return;
            }

            final boolean connectToExistedGame = Objects.nonNull(parameters.value().gameId());
            if (connectToExistedGame) {
                connectToExistedGame(session, parameters.value().gameId());
                return;
            }

            final boolean partnershipGame = Objects.nonNull(parameters.value().nameOfPartner());
            if (partnershipGame) {
                partnershipGame(session, username, parameters.value());
                return;
            }

            gameInit(session, username, parameters.value());
        });
    }

    private void connectToExistedGame(Session session, UUID gameId) {
        final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameId);
        pair.getSecond().add(session);

        sendMessage(session, JSONUtilities.chessGameToString(pair.getFirst()).orElseThrow());
    }

    private void gameInit(Session session, Username username, GameInit parameters) {
        final UserAccount firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();
        sendMessage(session, JSONUtilities.write(Message.info("Finding opponent... .")).orElseThrow());

        final GameParameters gameParameters = new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());

        final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = findOpponent(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            waitingForTheGame.put(username, Triple.of(session, firstPlayer, gameParameters));
            sendMessage(session, JSONUtilities.write(Message.info("Try to find opponent for you %s.".formatted(username.username()))).orElseThrow());
            return;
        }

        final Session secondSession = potentialOpponent.orElseThrow().getFirst();
        final UserAccount secondPlayer = potentialOpponent.orElseThrow().getSecond();
        final GameParameters secondGameParameters = potentialOpponent.orElseThrow().getThird();
        waitingForTheGame.remove(secondPlayer.getUsername());

        chessGameInitialization(session, firstPlayer, gameParameters, secondSession, secondPlayer, secondGameParameters);
    }

    private void partnershipGame(Session session, Username addresser, GameInit parameters) {
        Objects.requireNonNull(parameters.nameOfPartner());
        if (!outboundUserRepository.isUsernameExists(parameters.nameOfPartner())) {
            sendMessage(session, JSONUtilities.write(Message.error("User %s do not exists.".formatted(parameters.nameOfPartner().username()))).orElseThrow());
            return;
        }

        final UserAccount addresserAccount = outboundUserRepository.findByUsername(addresser).orElseThrow();

        final UserAccount addresseeAccount = Objects.requireNonNullElseGet(
                sessions.get(parameters.nameOfPartner()).getSecond(), () -> outboundUserRepository.findByUsername(parameters.nameOfPartner()).orElseThrow()
        );
        final String addressee = addresseeAccount.getUsername().username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        if (!isHavePartnership) {
            sendMessage(session, JSONUtilities.write(Message.error("You can`t invite someone who`s have not partnership with you.")).orElseThrow());
            return;
        }

        final GameParameters gameParametersOfAddresser = getGameParameters(parameters);
        partnershipGameCacheService.put(addressee, addresser.username(), gameParametersOfAddresser);

        final StatusPair<GameParameters> isPartnershipGameAgreed = isPartnershipGameAgreed(addressee, addresser.username());
        if (isPartnershipGameAgreed.status()) {
            partnershipGameInit(
                    sessions.get(addresser).getFirst(), addresserAccount, gameParametersOfAddresser,
                    sessions.get(addresseeAccount.getUsername()).getFirst(), addresseeAccount, isPartnershipGameAgreed.orElseThrow()
            );
        }
    }

    private StatusPair<GameParameters> isPartnershipGameAgreed(String addressee, String addresser) {
        final Map<String, GameParameters> requests = partnershipGameCacheService.getAll(addresser);
        if (requests.containsKey(addressee)) {
            return StatusPair.ofTrue(requests.get(addressee));
        }

        return StatusPair.ofFalse();
    }

    private void partnershipGameInit(Session addresser, UserAccount addresserAccount, GameParameters gameParametersOfAddresser,
                                     Session addressee, UserAccount addresseeAccount, GameParameters gameParametersOfAddressee) {

        Objects.requireNonNull(addresser);
        if (Objects.isNull(addressee)) {
            sendMessage(addresser, JSONUtilities.write(Message.error("The request timed out. Can`t initialize a game.")).orElseThrow());
            return;
        }

        final ChessGame chessGame = loadChessGame(addresserAccount, gameParametersOfAddresser, addresseeAccount, gameParametersOfAddressee);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>(Arrays.asList(addresser, addressee))));

        final Result<String, Throwable> overviewMessage = JSONUtilities.gameSessionToString(chessGame);
        sendMessage(addresser, overviewMessage.orElseThrow());
        sendMessage(addressee, overviewMessage.orElseThrow());

        final Result<String, Throwable> message = JSONUtilities.chessGameToString(chessGame);
        sendMessage(addresser, message.orElseThrow());
        sendMessage(addressee, message.orElseThrow());

        addresser.getUserProperties().put("game-id", new ArrayList<>(Collections.singletonList(chessGame.getChessGameId().toString())));
        addressee.getUserProperties().put("game-id", new ArrayList<>(Collections.singletonList(chessGame.getChessGameId().toString())));

        partnershipGameCacheService.delete(addresserAccount.getUsername().username(), addresseeAccount.getUsername().username());
        partnershipGameCacheService.delete(addresseeAccount.getUsername().username(), addresserAccount.getUsername().username());

        inboundChessRepository.completelySaveStartedChessGame(chessGame);
        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    private void move(Message move, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameSessions) {
        final String username = usernameSession.getFirst();
        final ChessGame chessGame = gameSessions.getFirst();

        try {
            chessGame.makeMovement(
                    username, move.from(), move.to(),
                    Objects.isNull(move.inCaseOfPromotion()) ? null : AlgebraicNotation.fromSymbol(move.inCaseOfPromotion())
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameSession.getSecond(), JSONUtilities.write(Message.error("Invalid chess movement.")).orElseThrow());
            return;
        }

        final String message = JSONUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void chat(Message message, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameSession.getFirst();

        try {
            ChatMessage chatMsg = new ChatMessage(message.message());
            gameAndSessions.getFirst().addChatMessage(username, chatMsg);
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, chatMsg.message()));
        } catch (IllegalArgumentException | NullPointerException e) {
            sendMessage(usernameSession.getSecond(), JSONUtilities.write(Message.error("Invalid message.")).orElseThrow());
        }
    }

    public void returnOfMovement(Pair<String, Session> usernameAndSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.returnMovement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), JSONUtilities.write(Message.error("Can`t return a move.")).orElseThrow());
            return;
        }

        if (!chessGame.isLastMoveWasUndo()) {
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, JSONUtilities.write(Message.info("Player {%s} requested for move returning.".formatted(username))).orElseThrow()));
            return;
        }

        final String message = JSONUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void resignation(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.resignation(username);

            String message = "Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, JSONUtilities.write(Message.info(message)).orElseThrow());
            }
        } catch (IllegalArgumentException e) {
            sendMessage(usernameAndSession.getSecond(), JSONUtilities.write(Message.error("Not a player.")).orElseThrow());
        }
    }

    public void threeFold(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), JSONUtilities.write(Message.error("Can`t end game by ThreeFold")).orElseThrow());
            return;
        }

        String message = "Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, JSONUtilities.write(Message.info(message)).orElseThrow());
        }
    }

    public void agreement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.agreement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), JSONUtilities.write(Message.error("Not a player. Illegal access.")).orElseThrow());
            return;
        }

        if (!chessGame.isAgreementAvailable()) {
            String message = "Player {%s} requested for agreement.".formatted(username);
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, JSONUtilities.write(Message.info(message)).orElseThrow()));

            return;
        }

        String message = "Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, JSONUtilities.write(Message.info(message)).orElseThrow());
        }
    }

    public void handleOnClose(Session session) {
        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            return;
        }

        for (String gameId : (List<String>) gameIdObj) {
            final UUID gameUuid = UUID.fromString(gameId);

            final boolean isGameSessionExists = gameSessions.containsKey(gameUuid);
            if (!isGameSessionExists) {
                sendMessage(session, JSONUtilities.write(Message.error("Game session with id {%s} does not exist".formatted(gameId))).orElseThrow());
                return;
            }

            final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameUuid);

            final ChessGame chessGame = pair.getFirst();
            if (chessGame.gameResult().isEmpty()) {
                return;
            }

            final Set<Session> sessionHashSet = pair.getSecond();
            final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().orElseThrow().toString());
            closeSession(session, JSONUtilities.write(Message.info(messageInCaseOfGameEnding)).orElseThrow());

            sessionHashSet.remove(session);
            if (sessionHashSet.isEmpty()) {
                gameSessions.remove(gameUuid);
            }
        }
    }

    private void chessGameInitialization(Session session, UserAccount firstPlayer, GameParameters gameParameters,
                                         Session secondSession,  UserAccount secondPlayer, GameParameters secondGameParameters) {
        final ChessGame chessGame = loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>(Arrays.asList(session, secondSession))));

        session.getUserProperties().put("game-id", new ArrayList<>(Collections.singletonList(chessGame.getChessGameId().toString())));
        secondSession.getUserProperties().put("game-id", new ArrayList<>(Collections.singletonList(chessGame.getChessGameId().toString())));

        final Result<String, Throwable> overviewMessage = JSONUtilities.gameSessionToString(chessGame);
        final Result<String, Throwable> message = JSONUtilities.chessGameToString(chessGame);

        sendMessage(session, overviewMessage.orElseThrow());
        sendMessage(secondSession, overviewMessage.orElseThrow());

        sendMessage(session, message.orElseThrow());
        sendMessage(secondSession, message.orElseThrow());

        inboundChessRepository.completelySaveStartedChessGame(chessGame);
        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }


    private static GameParameters getGameParameters(GameInit parameters) {
        return new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());
    }

    private StatusPair<Triple<Session, UserAccount, GameParameters>> findOpponent(final UserAccount firstPlayer, final GameParameters gameParameters) {
        for (var entry : waitingForTheGame.entrySet()) {
            final UserAccount potentialOpponent = entry.getValue().getSecond();
            final GameParameters gameParametersOfPotentialOpponent = entry.getValue().getThird();

            if (potentialOpponent.getId().equals(firstPlayer.getId())) {
                continue;
            }

            final boolean isOpponent = this.isOpponent(firstPlayer, gameParameters, potentialOpponent, gameParametersOfPotentialOpponent);
            if (isOpponent) {
                return StatusPair.ofTrue(entry.getValue());
            }

            final boolean waitToLong = gameParametersOfPotentialOpponent.waitingTime() > 9;
            if (waitToLong) {
                waitingForTheGame.remove(potentialOpponent.getUsername());
            }
        }

        return StatusPair.ofFalse();
    }

    private boolean isOpponent(final UserAccount player, final GameParameters gameParameters,
                               final UserAccount opponent, final GameParameters opponentGameParameters) {

        final boolean sameUser = player.getId().equals(opponent.getId());
        if (sameUser) {
            return false;
        }

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

    private ChessGame loadChessGame(final UserAccount firstPlayer, final GameParameters gameParameters,
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
