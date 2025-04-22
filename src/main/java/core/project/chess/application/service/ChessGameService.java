package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.application.dto.chess.PuzzleInbound;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.enumerations.MessageAddressee;
import core.project.chess.domain.chess.factories.ChessGameFactory;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.services.GameFunctionalityService;
import core.project.chess.domain.chess.services.PuzzleService;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.tuples.Triple;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.clients.PuzzlerClient;
import core.project.chess.infrastructure.dal.cache.GameInvitationsRepository;
import core.project.chess.infrastructure.dal.cache.SessionStorage;
import core.project.chess.infrastructure.security.JwtUtility;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ApplicationScoped
public class ChessGameService {

    private final JwtUtility jwtUtility;

    private final PuzzleService puzzleService;

    private final PuzzlerClient puzzlerClient;

    private final SessionStorage sessionStorage;

    private final ChessGameFactory chessGameFactory;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final GameFunctionalityService gameFunctionalityService;

    private final GameInvitationsRepository partnershipGameCacheService;

    ChessGameService(JwtUtility jwtUtility, PuzzleService puzzleService, PuzzlerClient puzzlerClient, SessionStorage sessionStorage,
                     ChessGameFactory chessGameFactory, InboundChessRepository inboundChessRepository,
                     OutboundUserRepository outboundUserRepository, GameFunctionalityService gameFunctionalityService,
                     GameInvitationsRepository partnershipGameCacheService) {
        this.jwtUtility = jwtUtility;
        this.puzzleService = puzzleService;
        this.puzzlerClient = puzzlerClient;
        this.sessionStorage = sessionStorage;
        this.chessGameFactory = chessGameFactory;
        this.inboundChessRepository = inboundChessRepository;
        this.outboundUserRepository = outboundUserRepository;
        this.gameFunctionalityService = gameFunctionalityService;
        this.partnershipGameCacheService = partnershipGameCacheService;
    }

    public void onOpen(Session session, Username username) {
        CompletableFuture.runAsync(() -> {
            Result<User, Throwable> result = outboundUserRepository.findByUsername(username.username());
            if (!result.success()) {
                closeSession(session, Message.error("This account is do not founded."));
                return;
            }

            sessionStorage.addSession(session, result.value());
            
            sendMessage(session, "connection successful");
            
            partnershipGameCacheService
                    .getAll(username.username())
                    .forEach((key, value) -> {
                        Message message = Message.invitation(key, value);
                        sendMessage(session, message);
                    });
        });
    }

    public void onMessage(Session session, Username username, Message message) {
        final boolean isRelatedToPuzzles = message.type().equals(MessageType.PUZZLE) || message.type().equals(MessageType.PUZZLE_MOVE);
        if (isRelatedToPuzzles) {
            handlePuzzleAction(session, username, message);
            return;
        }

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

        final Optional<ChessGame> chessGame = sessionStorage.getGameById(UUID.fromString(gameID.orElseThrow()));
        if (chessGame.isEmpty()) {
            sendMessage(session, Message.error("This game session does not exist."));
            return;
        }

        CompletableFuture.runAsync(
                () -> handleMessage(session, username.username(), message, chessGame.orElseThrow())
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

    private void handleMessage(final Session session, final String username, final Message message, final ChessGame chessGame) {
        final Pair<MessageAddressee, Message> result = switch (message.type()) {
            case MOVE -> gameFunctionalityService.move(message, Pair.of(username, session), chessGame);
            case MESSAGE -> gameFunctionalityService.chat(message, Pair.of(username, session), chessGame);
            case RETURN_MOVE -> gameFunctionalityService.returnOfMovement(Pair.of(username, session), chessGame);
            case RESIGNATION -> gameFunctionalityService.resignation(Pair.of(username, session), chessGame);
            case TREE_FOLD -> gameFunctionalityService.threeFold(Pair.of(username, session), chessGame);
            case AGREEMENT -> gameFunctionalityService.agreement(Pair.of(username, session), chessGame);
            default -> Pair.of(MessageAddressee.ONLY_ADDRESSER, Message.error("Invalid message type."));
        };

        final MessageAddressee messageAddressee = result.getFirst();
        final Message resultMessage = result.getSecond();
        if (messageAddressee.equals(MessageAddressee.ONLY_ADDRESSER)) {
            sendMessage(session, resultMessage);
            return;
        }

        sessionStorage.getGameSessions(chessGame.chessGameID())
                .forEach(gameSession -> sendMessage(gameSession, resultMessage));
    }

    private void handlePuzzleAction(Session session, Username username, Message message) {
        User user = sessionStorage.getSessionByUsername(username.username()).orElseThrow().getSecond();

        if (message.type().equals(MessageType.PUZZLE)) {
            sendMessage(session, puzzleService.chessPuzzle(user));
            return;
        }

        if (Objects.isNull(message.gameID())) {
            sendMessage(session, Message.error("Puzzle id is required for move."));
            return;
        }

        Result<UUID, Throwable> idResult = Result.ofThrowable(() -> UUID.fromString(message.gameID()));
        if (!idResult.success()) {
            sendMessage(session, Message.error("Puzzle id is invalid."));
            return;
        }

        Message response = puzzleService.puzzleMove(user, idResult.orElseThrow(),
                message.from(), message.to(), message.inCaseOfPromotion());
        sendMessage(session, response);
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
        final UUID gameId;
        try {
            gameId = UUID.fromString(gameID);
        } catch (IllegalArgumentException e) {
            sendMessage(session, Message.error("Invalid game ID."));
            return;
        }

        final Optional<ChessGame> chessGame = sessionStorage.getGameById(gameId);
        if (chessGame.isEmpty()) {
            sendMessage(session, Message.error("This game does not exist."));
            return;
        }

        sessionStorage.addSessionToGame(gameId, session);

        final ChessGame game = chessGame.orElseThrow();
        if (game.isPlayer(username)) {
            game.returnedToTheBoard(username);
            updateSessionGameIds(session, gameID);

            Message message = Message.builder(MessageType.INFO)
                    .gameID(gameID)
                    .message("Player %s returned to the game".formatted(username.username()))
                    .build();

            for (Session gameSession : sessionStorage.getGameSessions(game.chessGameID())) {
                sendMessage(gameSession, message);
            }
        }

        sendGameStartNotifications(session, game);
    }

    private void startNewGame(Session session, Username username, GameParameters gameParameters) {
        final User firstPlayer = outboundUserRepository.findByUsername(username.username()).orElseThrow();

        sendMessage(session, Message.userInfo("Finding opponent..."));

        final var potentialOpponent = locateOpponentForGame(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            sessionStorage.addWaitingUser(session, firstPlayer, gameParameters);

            Message message = Message.userInfo("Trying to find an opponent for you %s.".formatted(username.username()));
            sendMessage(session, message);
            return;
        }

        final Triple<Session, User, GameParameters> opponentData = potentialOpponent.orElseThrow();
        final User secondPlayer = opponentData.getSecond();

        sessionStorage.removeWaitingUser(secondPlayer.username());

        startStandardChessGame(
                Triple.of(session, firstPlayer, gameParameters), opponentData, false
        );
    }

    private void cancelGameSearch(Username username) {
        sessionStorage.removeWaitingUser(username.username());
    }

    private StatusPair<Triple<Session, User, GameParameters>> locateOpponentForGame(
            final User firstPlayer,
            final GameParameters gameParameters) {

        for (var entry : sessionStorage.waitingUsers()) {
            final Deque<Triple<Session, User, GameParameters>> queue = entry.getValue();
            for (var opponentTriple : queue) {
                final User potentialOpponent = opponentTriple.getSecond();
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

    private void handlePartnershipGameRequest(Session session, Username addresserUsername,
                                              GameParameters gameParameters, Message message) {
        final Result<Username, IllegalArgumentException> partnerUsername = message.partnerUsername();
        if (!partnerUsername.success()) {
            String errorMessage = "Invalid username for partner.%s".formatted(partnerUsername.throwable().getMessage());
            sendMessage(session, Message.error(errorMessage));
            return;
        }

        Username addresseeUsername = partnerUsername.orElseThrow();
        if (!outboundUserRepository.isUsernameExists(addresseeUsername.username())) {
            sendMessage(session, Message.error("User %s do not exists.".formatted(addresseeUsername)));
            return;
        }

        final User addresserAccount = outboundUserRepository.findByUsername(addresserUsername.username()).orElseThrow();

        final Optional<Pair<Session, User>> optionalSession = sessionStorage.getSessionByUsername(addresseeUsername.username());
        final User addresseeAccount = optionalSession.map(Pair::getSecond)
                .orElseGet(() -> outboundUserRepository.findByUsername(addresseeUsername.username()).orElseThrow());

        final String addressee = addresseeAccount.username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        Log.info("Is partnership for chess game exists: %s.".formatted(isHavePartnership));
        if (!isHavePartnership) {
            Log.error("Partnership not exists.");
            sendMessage(session, Message.error("You can`t invite someone who`s have not partnership with you."));
            return;
        }

        final boolean isRepeatedGameInvitation = partnershipGameCacheService.get(addressee, addresserUsername.username()).status();
        if (isRepeatedGameInvitation) {
            sendMessage(session, Message.error("You can't invite a user until they respond or the request expires."));
            return;
        }

        partnershipGameCacheService.put(addressee, addresserUsername.username(), gameParameters);

        final boolean isAddresseeActive = sessionStorage.containsSession(addresseeUsername.username());

        final boolean isRespondRequest = message.respond() != null && message.respond().equals(Message.Respond.YES);
        if (isRespondRequest) {
            handlePartnershipGameRespond(session, addresserAccount, addresseeAccount, gameParameters, isAddresseeActive);
            return;
        }

        final boolean isDeclineRequest = message.respond() != null && message.respond().equals(Message.Respond.NO);
        if (isDeclineRequest) {
            cancelRequests(addresserAccount, addresseeAccount);
            if (isAddresseeActive) {
                sessionStorage.getSessionByUsername(addresseeUsername.username())
                        .map(Pair::getFirst)
                        .ifPresent(addresseeSession -> sendMessage(addresseeSession, Message.userInfo(
                                "User %s has declined the partnership game.".formatted(addresseeUsername.username())
                        )));
            }
            return;
        }

        if (isAddresseeActive) {
            notifyTheAddressee(addresserUsername, addresseeUsername, gameParameters);
        }
    }

    private void handlePartnershipGameRespond(Session session, User addresserAccount, User addresseeAccount,
                                              GameParameters addresserGameParameters, boolean isAddresseeActive) {
        if (!isAddresseeActive) {
            cancelRequests(addresserAccount, addresseeAccount);
            sendMessage(session, Message.error("""
                    The game cannot be created because the user is not online.
                    You can try re-sending the partner game request when the user is online.
                    """
            ));
        }

        String addresserUsername = addresserAccount.username();
        String addresseeUsername = addresseeAccount.username();

        Map<String, GameParameters> partnershipGameRequests = partnershipGameCacheService.getAll(addresserUsername);
        if (!partnershipGameRequests.containsKey(addresseeUsername)) {
            sendMessage(session, Message.error("You can`t respond to partnership request if it don`t exist."));
            return;
        }

        GameParameters addresseeGameParameters = partnershipGameRequests.get(addresseeUsername);

        final boolean isOpponentEligible = gameFunctionalityService.validateOpponentEligibility(
                addresserAccount, addresserGameParameters, addresseeAccount, addresseeGameParameters, true
        );
        if (!isOpponentEligible) {
            sendMessage(session, "Opponent is do not eligible. Check the game parameters.");
            return;
        }

        cancelRequests(addresserAccount, addresseeAccount);

        Optional<Pair<Session, User>> addresseeSession = sessionStorage.getSessionByUsername(addresseeAccount.username());
        if (addresseeSession.isEmpty()) {
            cancelRequests(addresserAccount, addresseeAccount);
            sendMessage(session, Message.error("""
                    The game cannot be created because the user is not online.
                    You can try re-sending the partner game request when the user is online.
                    """
            ));
        }

        Log.info("Starting a partnership game.");
        startStandardChessGame(
                Triple.of(session, addresserAccount, addresserGameParameters),
                Triple.of(addresseeSession.orElseThrow().getFirst(), addresseeAccount, addresseeGameParameters),
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

        sendMessage(sessionStorage.getSessionByUsername(addresseeUsername.username()).orElseThrow().getFirst(), message);
    }

    private void cancelRequests(User firstPlayer, User secondPlayer) {
        partnershipGameCacheService.delete(firstPlayer.username(), secondPlayer.username());
        partnershipGameCacheService.delete(secondPlayer.username(), firstPlayer.username());
    }

    private void startStandardChessGame(Triple<Session, User, GameParameters> firstPlayerData,
                                        Triple<Session, User, GameParameters> secondPlayerData,
                                        final boolean isPartnershipGame) {
        Session firstSession = firstPlayerData.getFirst();
        User firstPlayer = firstPlayerData.getSecond();
        GameParameters firstGameParameters = firstPlayerData.getThird();

        Session secondSession = secondPlayerData.getFirst();
        User secondPlayer = secondPlayerData.getSecond();
        GameParameters secondGameParameters = secondPlayerData.getThird();

        ChessGame chessGame = chessGameFactory.createChessGameInstance(firstPlayer,
                firstGameParameters,
                secondPlayer,
                secondGameParameters,
                isPartnershipGame
        );

        registerGameAndNotifyPlayers(chessGame, firstSession, secondSession);

        if (isPartnershipGame) {
            cancelRequests(firstPlayer, secondPlayer);
        }

        inboundChessRepository.completelySaveStartedChessGame(chessGame);

        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    private void registerGameAndNotifyPlayers(ChessGame chessGame, Session firstSession, Session secondSession) {
        sessionStorage.addGame(chessGame, new HashSet<>(Arrays.asList(firstSession, secondSession)));

        sendGameStartNotifications(firstSession, chessGame);
        sendGameStartNotifications(secondSession, chessGame);

        String gameId = chessGame.chessGameID().toString();
        updateSessionGameIds(firstSession, gameId);
        updateSessionGameIds(secondSession, gameId);
    }

    @SuppressWarnings("User properties is always a list of strings.")
    private void updateSessionGameIds(Session session, String gameId) {
        final List<String> gameIds = (List<String>) session.getUserProperties()
                .computeIfAbsent("game-id", key -> new ArrayList<>());
        if (!gameIds.contains(gameId)) gameIds.add(gameId);
    }

    private void sendGameStartNotifications(Session session, ChessGame chessGame) {
        final Message overviewMessage = Message.builder(MessageType.GAME_START_INFO)
                .gameID(chessGame.chessGameID().toString())
                .whitePlayerUsername(chessGame.whitePlayer().username())
                .blackPlayerUsername(chessGame.blackPlayer().username())
                .whitePlayerRating(chessGame.whiteRating().rating())
                .blackPlayerRating(chessGame.blackRating().rating())
                .time(chessGame.time())
                .build();

        final Message message = Message.builder(MessageType.FEN_PGN)
                .gameID(chessGame.chessGameID().toString())
                .FEN(chessGame.fen())
                .PGN(chessGame.pgn())
                .build();

        sendMessage(session, overviewMessage);
        sendMessage(session, message);
    }

    public void onClose(Session session, Username username) {
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

            final Optional<ChessGame> chessGame = sessionStorage.getGameById(gameUuid);
            if (chessGame.isEmpty()) {
                continue;
            }
            if (!chessGame.orElseThrow().isGameOver()) {
                if (chessGame.get().isPlayer(username)) {
                    handleAFK(username, chessGame.get(), gameUuid);
                }
                continue;
            }

            final Set<Session> sessionHashSet = sessionStorage.getGameSessions(gameUuid);
            final String messageInCaseOfGameEnding = "Game ended. Because of %s"
                    .formatted(chessGame.orElseThrow().gameResult().toString());
            closeSession(session, Message.info(messageInCaseOfGameEnding));

            sessionHashSet.remove(session);
            if (sessionHashSet.isEmpty()) {
                sessionStorage.removeGame(gameUuid);
            }
        }

        sessionStorage.removeSession(session);
    }

    private void handleAFK(Username username, ChessGame chessGame, UUID gameUuid) {
        chessGame.awayFromTheBoard(username);
        Message message = Message.builder(MessageType.INFO)
                .gameID(chessGame.chessGameID().toString())
                .message("Player %s is AFK.".formatted(username.username()))
                .build();

        for (Session gameSession : sessionStorage.getGameSessions(gameUuid)) {
            sendMessage(gameSession, message);
        }
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
                try {
					Thread.sleep(Duration.ofMillis(100));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

                GameResult gameResult = game.gameResult();
                if (gameResult != GameResult.NONE) {
                    String message = "Game is over by result {%s}".formatted(gameResult);
                    Log.info(message);
                    Log.debugf("Removing game {%s}", game.chessGameID());
                    for (Session session : sessionStorage.getGameSessions(game.chessGameID())) {
                        sendMessage(session, Message.builder(MessageType.GAME_ENDED)
                                .message(message)
                                .gameID(game.chessGameID().toString())
                                .build());
                    }
                    sessionStorage.removeGame(game.chessGameID());

                    CompletableFuture.runAsync(() -> {
                        gameFunctionalityService.executeGameOverOperations(game);
                        PuzzleInbound puzzleInbound = puzzlerClient.sendPGN(game.pgn());
                        puzzleService.save(puzzleInbound.PGN(), puzzleInbound.startPositionOfPuzzle());
                    });
                    isRunning.set(false);
                }
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
