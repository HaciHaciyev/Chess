package core.project.chess.application.service;

import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.MessageAddressee;
import core.project.chess.domain.chess.factories.ChessGameFactory;
import core.project.chess.domain.chess.services.GameFunctionalityService;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.dal.cache.GameInvitationsRepository;
import core.project.chess.infrastructure.dal.cache.SessionStorage;
import core.project.chess.infrastructure.security.JwtUtility;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final JwtUtility jwtUtility;

    private final SessionStorage sessionStorage;

    private final ChessGameFactory chessGameFactory;

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final GameFunctionalityService gameFunctionalityService;

    private final GameInvitationsRepository partnershipGameCacheService;

    public void onOpen(Session session, Username username) {
        CompletableFuture.runAsync(() -> {
            Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
            if (!result.success()) {
                closeSession(session, Message.error("This account is do not founded."));
                return;
            }

            sessionStorage.addSession(session, result.value());
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

        final Optional<String> gameID = extractAndValidateGameID(session, message);
        if (gameID.isEmpty()) {
            sendMessage(session, Message.error("Can`t find a game id. Yoe need to provide game id,"));
            return;
        }

        final Pair<ChessGame, HashSet<Session>> gamePlusSessions = sessionStorage.getGameById(UUID.fromString(gameID.orElseThrow()));
        if (Objects.isNull(gamePlusSessions)) {
            sendMessage(session, Message.error("This game session does not exist."));
            return;
        }

        CompletableFuture.runAsync(
                () -> handleMessage(session, username.username(), message, gamePlusSessions)
        );
    }

    public Optional<JsonWebToken> validateToken(Session session) {
        return jwtUtility.extractJWT(session)
                .or(() -> {
                    closeSession(session, Message.error("You are not authorized. Token is required."));
                    sessionStorage.removeSession(session);
                    return Optional.empty();
                });
    }

    private static Optional<String> extractAndValidateGameID(Session session, Message message) {
        final String gameID = message.gameID();

        if (Objects.isNull(gameID) || gameID.isBlank()) {
            sendMessage(session, Message.error("Game id is required."));
            return Optional.empty();
        }

        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            sendMessage(session, Message.error("Game id is required."));
            return Optional.empty();
        }

        if (gameIdObj instanceof List<?> ls && !ls.contains(gameID)) {
            sendMessage(session, Message.error("This game id is do not exists."));
            return Optional.empty();
        }

        return gameID.describeConstable();
    }

    private void handleMessage(final Session session, final String username, final Message message,
                               final Pair<ChessGame, HashSet<Session>> gameSessions) {

        final Pair<MessageAddressee, Message> result = switch (message.type()) {
            case MOVE -> gameFunctionalityService.move(message, Pair.of(username, session), gameSessions);
            case MESSAGE -> gameFunctionalityService.chat(message, Pair.of(username, session), gameSessions);
            case RETURN_MOVE -> gameFunctionalityService.returnOfMovement(Pair.of(username, session), gameSessions);
            case RESIGNATION -> gameFunctionalityService.resignation(Pair.of(username, session), gameSessions);
            case TREE_FOLD -> gameFunctionalityService.threeFold(Pair.of(username, session), gameSessions);
            case AGREEMENT -> gameFunctionalityService.agreement(Pair.of(username, session), gameSessions);
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
                joinExistingGameSession(session, username, message.gameID());
                return;
            }

            final Result<GameParameters, IllegalArgumentException> gameParameters = message.gameParameters();
            if (!gameParameters.success()) {
                sendMessage(session, Message.error("Invalid game parameters."));
                return;
            }

            final boolean partnershipGame = Objects.nonNull(message.partner());
            if (partnershipGame) {
                handlePartnershipGameRequest(session, username, gameParameters.orElseThrow(), message);
                return;
            }

            final boolean isGameSearchCanceling = Objects.nonNull(message.respond()) && message.respond().equals(Message.Respond.NO);
            if (isGameSearchCanceling) {
                cancelGameSearch(username);
                return;
            }

            startNewGame(session, username, gameParameters.orElseThrow());
        });
    }

    private void joinExistingGameSession(Session session, Username username, String gameID) {
        final Pair<ChessGame, HashSet<Session>> gameAndHisSessions = sessionStorage.getGameById(UUID.fromString(gameID));
        gameAndHisSessions.getSecond().add(session);

        final ChessGame game = gameAndHisSessions.getFirst();
        final boolean isAPlayer = game.getPlayerForWhite().getUsername().equals(username) || game.getPlayerForBlack().getUsername().equals(username);
        if (isAPlayer) {
            updateSessionGameIds(session, gameID);
        }

        sendGameStartNotifications(session, gameAndHisSessions.getFirst());
    }

    private void startNewGame(Session session, Username username, GameParameters gameParameters) {
        final UserAccount firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();

        sendMessage(session, Message.userInfo("Finding opponent..."));

        final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = locateOpponentForGame(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            sessionStorage.addWaitingUser(session, firstPlayer, gameParameters);

            Message message = Message.userInfo("Trying to find an opponent for you %s.".formatted(username.username()));
            sendMessage(session, message);
            return;
        }

        final Triple<Session, UserAccount, GameParameters> opponentData = potentialOpponent.orElseThrow();
        final UserAccount secondPlayer = opponentData.getSecond();

        sessionStorage.removeWaitingUser(secondPlayer.getUsername());

        startStandardChessGame(
                Triple.of(session, firstPlayer, gameParameters), opponentData, false
        );
    }

    private void cancelGameSearch(Username username) {
        sessionStorage.removeWaitingUser(username);
    }

    private StatusPair<Triple<Session, UserAccount, GameParameters>> locateOpponentForGame(final UserAccount firstPlayer,
                                                                                           final GameParameters gameParameters) {
        for (var entry : sessionStorage.waitingUsers()) {
            final Queue<Triple<Session, UserAccount, GameParameters>> queue = entry.getValue();
            for (var opponentTriple : queue) {
                final UserAccount potentialOpponent = opponentTriple.getSecond();
                final GameParameters gameParametersOfPotentialOpponent = opponentTriple.getThird();

                final boolean isOpponent = gameFunctionalityService.validateOpponentEligibility(firstPlayer,
                        gameParameters,
                        potentialOpponent,
                        gameParametersOfPotentialOpponent,
                        false
                );

                if (isOpponent) {
                    return StatusPair.ofTrue(opponentTriple);
                }
            }
        }

        return StatusPair.ofFalse();
    }

    private void handlePartnershipGameRequest(Session session, Username addresserUsername, GameParameters gameParameters, Message message) {
        final Result<Username, IllegalArgumentException> partnerUsername = message.partnerUsername();
        if (!partnerUsername.success()) {
            String errorMessage = "Invalid username for partner.%s".formatted(partnerUsername.throwable().getMessage());
            sendMessage(session, Message.error(errorMessage));
            return;
        }

        Username addresseeUsername = partnerUsername.orElseThrow();
        if (!outboundUserRepository.isUsernameExists(addresseeUsername)) {
            sendMessage(session, Message.error("User %s do not exists.".formatted(addresseeUsername)));
            return;
        }

        final UserAccount addresserAccount = outboundUserRepository.findByUsername(addresserUsername).orElseThrow();

        final UserAccount addresseeAccount = Objects.requireNonNullElseGet(
                sessionStorage.getSessionByUsername(addresseeUsername).getSecond(), () -> outboundUserRepository.findByUsername(addresseeUsername).orElseThrow()
        );
        final String addressee = addresseeAccount.getUsername().username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        if (!isHavePartnership) {
            sendMessage(session, Message.error("You can`t invite someone who`s have not partnership with you."));
            return;
        }

        final boolean isRepeatedGameInvitation = partnershipGameCacheService.get(addressee, addresserUsername.username()).status();
        if (isRepeatedGameInvitation) {
            sendMessage(session, Message.error("You can't invite a user until they respond or the request expires."));
            return;
        }

        partnershipGameCacheService.put(addressee, addresserUsername.username(), gameParameters);

        final boolean isAddresseeActive = sessionStorage.containsSession(addresseeUsername);

        final boolean isRespondRequest = message.respond() != null && message.respond().equals(Message.Respond.YES);
        if (isRespondRequest) {
            handlePartnershipGameRespond(session, addresserAccount, addresseeAccount, gameParameters, isAddresseeActive);
            return;
        }

        final boolean isDeclineRequest = message.respond() != null && message.respond().equals(Message.Respond.NO);
        if (isDeclineRequest) {
            cancelRequests(addresserAccount, addresseeAccount);
            if (isAddresseeActive) {
                Session addresseeSession = sessionStorage.getSessionByUsername(addresseeUsername).getFirst();
                sendMessage(addresseeSession, Message.userInfo("User %s has declined the partnership game.".formatted(addresseeUsername.username())));
            }
            return;
        }

        if (isAddresseeActive) {
            notifyTheAddressee(addresserUsername, addresseeUsername, gameParameters);
        }
    }

    private void handlePartnershipGameRespond(Session session, UserAccount addresserAccount, UserAccount addresseeAccount,
                                              GameParameters addresserGameParameters, boolean isAddresseeActive) {
        if (!isAddresseeActive) {
            cancelRequests(addresserAccount, addresseeAccount);
            sendMessage(session, Message.error("""
                    The game cannot be created because the user is not online.
                    You can try re-sending the partner game request when the user is online.
                    """
            ));
        }

        Username addresserUsername = addresserAccount.getUsername();
        Username addresseeUsername = addresseeAccount.getUsername();

        Map<String, GameParameters> partnershipGameRequests = partnershipGameCacheService.getAll(addresserUsername.username());
        if (!partnershipGameRequests.containsKey(addresseeUsername.username())) {
            sendMessage(session, Message.error("You can`t respond to partnership request if it don`t exist."));
            return;
        }

        GameParameters addresseeGameParameters = partnershipGameRequests.get(addresseeUsername.username());

        final boolean isOpponentEligible = gameFunctionalityService.validateOpponentEligibility(
                addresserAccount, addresserGameParameters, addresseeAccount, addresseeGameParameters, true
        );
        if (!isOpponentEligible) {
            sendMessage(session, "Opponent is do not eligible. Check the game parameters.");
            return;
        }

        cancelRequests(addresserAccount, addresseeAccount);

        Session addresseeSession = sessionStorage.getSessionByUsername(addresseeAccount.getUsername()).getFirst();
        startStandardChessGame(
                Triple.of(session, addresserAccount, addresserGameParameters),
                Triple.of(addresseeSession, addresseeAccount, addresseeGameParameters),
                true
        );
    }

    private void notifyTheAddressee(Username addresserUsername, Username addresseeUsername, GameParameters gameParameters) {
        Color color = null;
        if (Objects.nonNull(gameParameters.color())) {
            color = gameParameters.color().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        }

        Message message = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .message("User {%s} invite you for partnership game.".formatted(addresserUsername.username()))
                .time(gameParameters.time())
                .FEN(gameParameters.FEN())
                .PGN(gameParameters.PGN())
                .color(color)
                .build();

        sendMessage(sessionStorage.getSessionByUsername(addresseeUsername).getFirst(), message);
    }

    private void cancelRequests(UserAccount firstPlayer, UserAccount secondPlayer) {
        partnershipGameCacheService.delete(firstPlayer.getUsername().username(), secondPlayer.getUsername().username());
        partnershipGameCacheService.delete(secondPlayer.getUsername().username(), firstPlayer.getUsername().username());
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

        Result<ChessGame, IllegalArgumentException> chessGame = chessGameFactory.createChessGameInstance(firstPlayer,
                firstGameParameters,
                secondPlayer,
                secondGameParameters
        );

        if (!chessGame.success()) {
            Message errorMessage = Message.error("Can`t create a chess game.%s".formatted(chessGame.throwable().getMessage()));
            sendMessage(firstSession, errorMessage);
            sendMessage(secondSession, errorMessage);
            return;
        }

        registerGameAndNotifyPlayers(chessGame.orElseThrow(), firstSession, secondSession);

        if (isPartnershipGame) {
            cancelRequests(firstPlayer, secondPlayer);
        }

        inboundChessRepository.completelySaveStartedChessGame(chessGame.orElseThrow());

        ChessGameSpectator spectator = new ChessGameSpectator(chessGame.orElseThrow());
        spectator.start();
    }

    private void registerGameAndNotifyPlayers(ChessGame chessGame, Session firstSession, Session secondSession) {
        sessionStorage.addGame(chessGame, new HashSet<>(Arrays.asList(firstSession, secondSession)));

        sendGameStartNotifications(firstSession, chessGame);
        sendGameStartNotifications(secondSession, chessGame);

        String gameId = chessGame.getChessGameId().toString();
        updateSessionGameIds(firstSession, gameId);
        updateSessionGameIds(secondSession, gameId);
    }

    @SuppressWarnings("User properties is always a list of strings.")
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
                .time(chessGame.getTime())
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

            final boolean isGameSessionExists = sessionStorage.containsGame(gameUuid);
            if (!isGameSessionExists) {
                sendMessage(session, Message.error("Game session with id {%s} does not exist".formatted(gameId)));
                continue;
            }

            final Pair<ChessGame, HashSet<Session>> pair = sessionStorage.getGameById(gameUuid);

            final ChessGame chessGame = pair.getFirst();
            if (chessGame.gameResult().isEmpty()) {
                continue;
            }

            final Set<Session> sessionHashSet = pair.getSecond();
            final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().orElseThrow().toString());
            closeSession(session, Message.info(messageInCaseOfGameEnding));

            sessionHashSet.remove(session);
            if (sessionHashSet.isEmpty()) {
                sessionStorage.removeGame(gameUuid);
            }
        }

        sessionStorage.removeSession(session);
    }

    private void gameOverOperationsExecutor(final ChessGame chessGame) {
        if (outboundChessRepository.isChessHistoryPresent(chessGame.getChessBoard().getChessBoardId())) {
            Log.infof("History of game %s is already present", chessGame.getChessGameId());
            return;
        }

        Log.infof("Saving finished game %s and changing ratings", chessGame.getChessGameId());
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);

        if (chessGame.isCasualGame()) {
            return;
        }

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
                    var gameAndSessions = sessionStorage.removeGame(game.getChessGameId());

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
