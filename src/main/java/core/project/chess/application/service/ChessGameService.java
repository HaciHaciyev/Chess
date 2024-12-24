package core.project.chess.application.service;

import core.project.chess.application.dto.chess.GameParameters;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.domain.subdomains.chess.entities.ChessBoard;
import core.project.chess.domain.subdomains.chess.entities.ChessGame;
import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.MessageAddressee;
import core.project.chess.domain.subdomains.chess.events.SessionEvents;
import core.project.chess.domain.subdomains.chess.services.GameFunctionalityService;
import core.project.chess.domain.subdomains.user.entities.UserAccount;
import core.project.chess.domain.subdomains.user.value_objects.Username;
import core.project.chess.infrastructure.dal.cache.GameInvitationsRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final GameFunctionalityService gameFunctionalityService;

    private final GameInvitationsRepository partnershipGameCacheService;

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
                        Message message = Message.invitation(key, value);
                        sendMessage(session, message);
                    });
        });
    }

    public void onMessage(Session session, Username username, Message message) {
        final boolean isGameInitialization = message.type().equals(MessageType.GAME_INIT);
        if (isGameInitialization) {
            initializeGameSession(session, username, message);
            return;
        }

        final String gameID = message.gameID();
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
                () -> handleWebSocketMessage(session, username.username(), message, gamePlusSessions)
        );
    }

    private void handleWebSocketMessage(final Session session, final String username, final Message message,
                                        final Pair<ChessGame, HashSet<Session>> gameSessions) {

        final Pair<MessageAddressee, Message> result = switch (message.type()) {
            case MOVE -> this.gameFunctionalityService.move(message, Pair.of(username, session), gameSessions);
            case MESSAGE -> this.gameFunctionalityService.chat(message, Pair.of(username, session), gameSessions);
            case RETURN_MOVE -> this.gameFunctionalityService.returnOfMovement(Pair.of(username, session), gameSessions);
            case RESIGNATION -> this.gameFunctionalityService.resignation(Pair.of(username, session), gameSessions);
            case TREE_FOLD -> this.gameFunctionalityService.threeFold(Pair.of(username, session), gameSessions);
            case AGREEMENT -> this.gameFunctionalityService.agreement(Pair.of(username, session), gameSessions);
            default -> Pair.of(MessageAddressee.ONLY_ADDRESSER, Message.error("Invalid message type."));
        };

        final MessageAddressee messageAddressee = result.getFirst();
        final Message resultMessage = result.getSecond();

        if (messageAddressee.equals(MessageAddressee.ONLY_ADDRESSER)) {
            sendMessage(session, resultMessage);
            return;
        }

        gameSessions.getSecond().forEach(currentSession -> sendMessage(currentSession, resultMessage));
    }

    private void initializeGameSession(Session session, Username username, Message message) {
        CompletableFuture.runAsync(() -> {
            final boolean connectToExistedGame = Objects.nonNull(message.gameID());
            if (connectToExistedGame) {
                joinExistingGameSession(session, message.gameID());
                return;
            }

            final Result<GameParameters, IllegalArgumentException> gameParameters = message.gameParameters();
            if (!gameParameters.success()) {
                sendMessage(session, Message.error("Invalid game parameters."));
                return;
            }

            final boolean partnershipGame = Objects.nonNull(message.partner());
            if (partnershipGame) {
                final Result<Username, IllegalArgumentException> partnerUsername = message.partnerUsername();
                if (!partnerUsername.success()) {
                    String errorMessage = "Invalid username for partner.%s".formatted(partnerUsername.throwable().getMessage());
                    sendMessage(session, Message.error(errorMessage));
                    return;
                }

                handlePartnershipGameRequest(session, username, partnerUsername.orElseThrow(), gameParameters.orElseThrow());
                return;
            }

            startNewGame(session, username, gameParameters.orElseThrow());
        });
    }

    private void joinExistingGameSession(Session session, String gameID) {
        final Pair<ChessGame, HashSet<Session>> gameAndHisSessions = gameSessions.get(UUID.fromString(gameID));
        gameAndHisSessions.getSecond().add(session);

        sendGameStartNotifications(session, gameAndHisSessions.getFirst());
    }

    private void startNewGame(Session session, Username username, GameParameters gameParameters) {
        final UserAccount firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();

        sendMessage(session, Message.info("Finding opponent..."));

        final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = locateOpponentForGame(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            waitingForTheGame.put(username, Triple.of(session, firstPlayer, gameParameters));

            Message message = Message.userInfo("Trying to find an opponent for you %s.".formatted(username.username()));
            sendMessage(session, message);
            return;
        }

        final Triple<Session, UserAccount, GameParameters> opponentData = potentialOpponent.orElseThrow();
        final UserAccount secondPlayer = opponentData.getSecond();

        waitingForTheGame.remove(secondPlayer.getUsername());

        startStandardChessGame(
                Triple.of(session, firstPlayer, gameParameters), opponentData, false
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
        assert gameParameters.timeControllingTYPE() != null;
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

    private void handlePartnershipGameRequest(Session session, Username addresserUsername, Username addresseeUsername, GameParameters gameParameters) {
        if (!outboundUserRepository.isUsernameExists(addresseeUsername)) {
            sendMessage(session, Message.error("User %s do not exists.".formatted(addresseeUsername)));
            return;
        }

        final UserAccount addresserAccount = outboundUserRepository.findByUsername(addresserUsername).orElseThrow();

        final UserAccount addresseeAccount = Objects.requireNonNullElseGet(
                sessions.get(addresseeUsername).getSecond(), () -> outboundUserRepository.findByUsername(addresseeUsername).orElseThrow()
        );
        final String addressee = addresseeAccount.getUsername().username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        if (!isHavePartnership) {
            sendMessage(session, Message.error("You can`t invite someone who`s have not partnership with you."));
            return;
        }

        partnershipGameCacheService.put(addressee, addresserUsername.username(), gameParameters);

        final StatusPair<GameParameters> isPartnershipGameAgreed = checkPartnershipAgreement(addressee, addresserUsername.username());
        if (isPartnershipGameAgreed.status()) {
            var addresserSession = sessions.get(addresserUsername).getFirst();
            var addresseeSession = sessions.get(addresseeAccount.getUsername()).getFirst();

            startStandardChessGame(
                    Triple.of(addresserSession, addresserAccount, gameParameters),
                    Triple.of(addresseeSession, addresseeAccount, gameParameters),
                    true
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

    private void startStandardChessGame(Triple<Session, UserAccount, GameParameters> firstPlayerData,
                                        Triple<Session, UserAccount, GameParameters> secondPlayerData,
                                        final boolean isPartnershipGame) {
        Session firstSession = firstPlayerData.getFirst();
        UserAccount firstPlayer = firstPlayerData.getSecond();
        GameParameters firstGameParameters = firstPlayerData.getThird();

        Session secondSession = secondPlayerData.getFirst();
        UserAccount secondPlayer = secondPlayerData.getSecond();
        GameParameters secondGameParameters = secondPlayerData.getThird();

        ChessGame chessGame = createChessGameInstance(firstPlayer, firstGameParameters, secondPlayer, secondGameParameters);
        registerGameAndNotifyPlayers(chessGame, firstSession, secondSession);

        if (isPartnershipGame) {
            partnershipGameCacheService.delete(firstPlayer.getUsername().username(), secondPlayer.getUsername().username());
            partnershipGameCacheService.delete(secondPlayer.getUsername().username(), firstPlayer.getUsername().username());
        }

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
        final List<String> gameIds = (List<String>) session.getUserProperties().computeIfAbsent("game-id", key -> new ArrayList<>());
        if (!gameIds.contains(gameId)) {
            gameIds.add(gameId);
        }
    }

    private void sendGameStartNotifications(Session session, ChessGame chessGame) {
        final Message overviewMessage = Message.builder(MessageType.GAME_START_INFO)
                .gameID(chessGame.getChessGameId().toString())
                .whitePlayerUsername(chessGame.getPlayerForWhite().getUsername())
                .blackPlayerUsername(chessGame.getPlayerForBlack().getUsername())
                .whitePlayerRating(chessGame.getPlayerForWhiteRating().rating())
                .blackPlayerRating(chessGame.getPlayerForBlackRating().rating())
                .time(chessGame.getTimeControllingTYPE())
                .build();

        final Message message = Message.builder(MessageType.FEN_PGN)
                .gameID(chessGame.getChessGameId().toString())
                .FEN(chessGame.getChessBoard().actualRepresentationOfChessBoard())
                .PGN(chessGame.getChessBoard().pgn())
                .build();

        sendMessage(session, overviewMessage);
        sendMessage(session, message);
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

        public ChessGameSpectator(ChessGame game) {
            this.game = game;
            this.isRunning = new AtomicBoolean(false);
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
                        sendMessage(session, Message.info("Game is over by result {%s}".formatted(gameResult)));
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
            Thread.startVirtualThread(this);
        }
    }
}
