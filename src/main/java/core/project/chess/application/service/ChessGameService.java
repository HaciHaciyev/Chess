package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
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
import core.project.chess.domain.commons.value_objects.GameRequest;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.clients.PuzzlerClient;
import core.project.chess.infrastructure.dal.cache.GameInvitationsRepository;
import core.project.chess.infrastructure.dal.cache.SessionStorage;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ApplicationScoped
public class ChessGameService {

    private final PuzzleService puzzleService;

    private final PuzzlerClient puzzlerClient;

    private final SessionStorage sessionStorage;

    private final ChessGameFactory chessGameFactory;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final GameFunctionalityService gameFunctionalityService;

    private final GameInvitationsRepository partnershipGameCacheService;

    ChessGameService(PuzzleService puzzleService, PuzzlerClient puzzlerClient, SessionStorage sessionStorage,
                     ChessGameFactory chessGameFactory, InboundChessRepository inboundChessRepository,
                     OutboundUserRepository outboundUserRepository, GameFunctionalityService gameFunctionalityService,
                     GameInvitationsRepository partnershipGameCacheService) {
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
        Result<User, Throwable> result = outboundUserRepository.findByUsername(username);
        if (!result.success()) {
            closeSession(session, Message.error("This account is do not founded."));
            return;
        }
        if (sessionStorage.containsSession(username)) {
            closeSession(session, Message.error("You already have active session."));
            return;
        }

        sessionStorage.addSession(session, result.value());
        
        sendMessage(session, Message.info("Successful connection to chessland"));
        partnershipGameCacheService
                .getAll(username)
                .forEach((key, value) -> {
                    Message message = Message.invitation(key, value);
                    sendMessage(session, message);
                });
    }

    public void onMessage(Session session, Username username, Message message) {
        final boolean isRelatedToPuzzles = message.type() == MessageType.PUZZLE || message.type() == MessageType.PUZZLE_MOVE;
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

        handleMessage(session, username.username(), message, chessGame.orElseThrow());
    }

    public Optional<Pair<Session, User>> user(Username username) {
        return sessionStorage.getSessionByUsername(username);
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
        User user = sessionStorage.getSessionByUsername(username).orElseThrow().getSecond();

        if (message.type().equals(MessageType.PUZZLE)) {
            Puzzle puzzle = puzzleService.chessPuzzle(user);
            sendMessage(session, Message.builder(MessageType.PUZZLE)
                    .gameID(puzzle.ID().toString())
                    .FEN(puzzle.chessBoard().toString())
                    .PGN(puzzle.chessBoard().pgn())
                    .build());
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

        Optional<Puzzle> puzzle = sessionStorage.getPuzzle(new Username(user.username()), idResult.value());
        if (puzzle.isEmpty()) {
            sendMessage(session, Message.error("This puzzle session do not exists."));
            return;
        }

        Message response = puzzleService.puzzleMove(puzzle.get(), message.from(), message.to(), message.inCaseOfPromotion());
        sendMessage(session, response);
    }

    private void initializeGameSession(Session session, Username username, Message message) {
        final boolean connectToExistingGame = Objects.nonNull(message.gameID());
        if (connectToExistingGame) {
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

            for (Session gameSession : sessionStorage.getGameSessions(game.chessGameID()))
                sendMessage(gameSession, message);
        }

        sendGameStartNotifications(session, game);
    }

    private void startNewGame(Session session, Username username, GameParameters gameParameters) {
        final User firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();

        sendMessage(session, Message.userInfo("Finding opponent..."));

        final var potentialOpponent = locateOpponentForGame(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            sessionStorage.addWaitingUser(new GameRequest(session, firstPlayer, gameParameters));

            Message message = Message.userInfo("Trying to find an opponent for you %s.".formatted(username.username()));
            sendMessage(session, message);
            return;
        }

        final GameRequest opponentData = potentialOpponent.orElseThrow();
        final User secondPlayer = opponentData.user();

        startStandardChessGame(
                new GameRequest(session, firstPlayer, gameParameters), opponentData, false
        );
    }

    private void cancelGameSearch(Username username) {
        sessionStorage.removeLastGameSearchRequestOf(username);
    }

    private StatusPair<GameRequest> locateOpponentForGame(
            final User firstPlayer,
            final GameParameters gameParameters) {
        for (var entry : sessionStorage.waitingUsers()) {
            for (GameRequest waitingUser : entry.getValue()) {
                final User potentialOpponent = waitingUser.user();
                final GameParameters gameParametersOfPotentialOpponent = waitingUser.gameParameters();

                final boolean isOpponent = gameFunctionalityService.validateOpponentEligibility(firstPlayer,
                    gameParameters,
                    potentialOpponent,
                    gameParametersOfPotentialOpponent,
                    false
                );

                if (isOpponent && sessionStorage.removeWaitingUser(waitingUser))
                    return StatusPair.ofTrue(waitingUser);
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
        if (!outboundUserRepository.isUsernameExists(addresseeUsername)) {
            sendMessage(session, Message.error("User %s do not exists.".formatted(addresseeUsername)));
            return;
        }

        final User addresserAccount = outboundUserRepository.findByUsername(addresserUsername).orElseThrow();

        final Optional<Pair<Session, User>> optionalSession = sessionStorage.getSessionByUsername(addresseeUsername);
        final User addresseeAccount = optionalSession.map(Pair::getSecond)
                .orElseGet(() -> outboundUserRepository.findByUsername(addresseeUsername).orElseThrow());

        final String addressee = addresseeAccount.username();

        final boolean isHavePartnership = outboundUserRepository.havePartnership(addresseeAccount, addresserAccount);
        if (!isHavePartnership) {
            Log.error("Partnership not exists.");
            sendMessage(session, Message.error("You can`t invite someone who`s have not partnership with you."));
            return;
        }

        final boolean isRepeatedGameInvitation = partnershipGameCacheService.get(new Username(addressee), addresserUsername).status();
        if (isRepeatedGameInvitation) {
            sendMessage(session, Message.error("You can't invite a user until they respond or the request expires."));
            return;
        }

        partnershipGameCacheService.put(new Username(addressee), addresserUsername, gameParameters);

        final boolean isAddresseeActive = sessionStorage.containsSession(addresseeUsername);

        final boolean isRespondRequest = message.respond() != null && message.respond().equals(Message.Respond.YES);
        if (isRespondRequest) {
            handlePartnershipGameRespond(session, addresserAccount, addresseeAccount, gameParameters, isAddresseeActive);
            return;
        }

        final boolean isDeclineRequest = message.respond() != null && message.respond().equals(Message.Respond.NO);
        if (isDeclineRequest) {
            cancelRequests(addresserAccount, addresseeAccount);
            if (isAddresseeActive)
                sessionStorage.getSessionByUsername(addresseeUsername)
                    .map(Pair::getFirst)
                    .ifPresent(addresseeSession -> sendMessage(addresseeSession, Message.userInfo(
                            "User %s has declined the partnership game.".formatted(addresseeUsername.username())
                    )));
            return;
        }

        if (isAddresseeActive) notifyTheAddressee(addresserUsername, addresseeUsername, gameParameters);
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

        Map<String, GameParameters> partnershipGameRequests = partnershipGameCacheService.getAll(new Username(addresserUsername));
        if (!partnershipGameRequests.containsKey(addresseeUsername)) {
            sendMessage(session, Message.error("You can`t respond to partnership request if it don`t exist."));
            return;
        }

        GameParameters addresseeGameParameters = partnershipGameRequests.get(addresseeUsername);

        final boolean isOpponentEligible = gameFunctionalityService.validateOpponentEligibility(
                addresserAccount, addresserGameParameters, addresseeAccount, addresseeGameParameters, true
        );
        if (!isOpponentEligible) {
            sendMessage(session, Message.error("Opponent is do not eligible. Check the game parameters."));
            return;
        }

        cancelRequests(addresserAccount, addresseeAccount);

        Username addresseeUserName = new Username(addresseeAccount.username());
        Optional<Pair<Session, User>> addresseeSession = sessionStorage.getSessionByUsername(addresseeUserName);
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
                new GameRequest(session, addresserAccount, addresserGameParameters),
                new GameRequest(addresseeSession.orElseThrow().getFirst(), addresseeAccount, addresseeGameParameters),
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

        sendMessage(sessionStorage.getSessionByUsername(addresseeUsername).orElseThrow().getFirst(), message);
    }

    private void cancelRequests(User firstPlayer, User secondPlayer) {
        partnershipGameCacheService.delete(new Username(firstPlayer.username()), new Username(secondPlayer.username()));
        partnershipGameCacheService.delete(new Username(secondPlayer.username()), new Username(firstPlayer.username()));
    }

    private void startStandardChessGame(GameRequest firstPlayerData,
                                        GameRequest secondPlayerData,
                                        final boolean isPartnershipGame) {
        Session firstSession = firstPlayerData.session();
        User firstPlayer = firstPlayerData.user();
        GameParameters firstGameParameters = firstPlayerData.gameParameters();

        Session secondSession = secondPlayerData.session();
        User secondPlayer = secondPlayerData.user();
        GameParameters secondGameParameters = secondPlayerData.gameParameters();

        Result<ChessGame, Throwable> chessGame = chessGameFactory.createChessGameInstance(firstPlayer,
                firstGameParameters,
                secondPlayer,
                secondGameParameters,
                isPartnershipGame
        );
        if (!chessGame.success()) {
            Message error = Message.error("Can`t create a chess game instance. Invalid game parameters provided.");
            sendMessage(firstSession, error);
            sendMessage(secondSession, error);
            return;
        }

        registerGameAndNotifyPlayers(chessGame.value(), firstSession, secondSession);

        if (isPartnershipGame) {
            cancelRequests(firstPlayer, secondPlayer);
        }

        inboundChessRepository.completelySaveStartedChessGame(chessGame.value());

        ChessGameSpectator spectator = new ChessGameSpectator(chessGame.value());
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
        final List<String> gameIds = (List<String>) session.getUserProperties().computeIfAbsent("game-id", key -> new ArrayList<>());
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
        if (!sessionStorage.containsSession(username)) return;

        final Object gameIdObj = session.getUserProperties().get("game-id");
        if (Objects.isNull(gameIdObj)) {
            sessionStorage.removeSession(username);
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
            if (chessGame.isEmpty()) continue;

            if (!chessGame.get().isGameOver()) {
                if (chessGame.get().isPlayer(username)) handleAFK(username, chessGame.get(), gameUuid);
                continue;
            }

            final Set<Session> sessionHashSet = sessionStorage.getGameSessions(gameUuid);
            sessionHashSet.remove(session);
            if (sessionHashSet.isEmpty()) sessionStorage.removeGame(gameUuid);

            final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.get().gameResult().toString());
            sendMessage(session, Message.info(messageInCaseOfGameEnding));
        }

        sessionStorage.removeSession(username);
    }

    private void handleAFK(Username username, ChessGame chessGame, UUID gameUuid) {
        chessGame.awayFromTheBoard(username);
        Message message = Message.builder(MessageType.INFO)
                .gameID(chessGame.chessGameID().toString())
                .message("Player %s is AFK.".formatted(username.username()))
                .build();

        for (Session gameSession : sessionStorage.getGameSessions(gameUuid)) sendMessage(gameSession, message);
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
                if (gameResult == GameResult.NONE) continue;

                String message = "Game is over by result {%s}".formatted(gameResult);
                Log.info(message);
                Log.debugf("Removing game {%s}", game.chessGameID());

                for (Session session : sessionStorage.getGameSessions(game.chessGameID()))
                    sendMessage(session, Message.builder(MessageType.GAME_ENDED)
                            .message(message)
                            .gameID(game.chessGameID().toString())
                            .build());

                sessionStorage.removeGame(game.chessGameID());

                CompletableFuture.supplyAsync(() -> {
                    gameFunctionalityService.executeGameOverOperations(game);
                    return puzzlerClient.sendPGN(game.pgn());
                }).thenAccept(puzzle -> {
                    if (Objects.nonNull(puzzle))
                        puzzleService.save(puzzle.moves(), puzzle.startPositionOfPuzzle());
                }).whenComplete((result, throwable) -> {
                    if (throwable != null) Log.error("Error during puzzle receive or save.", throwable);
                });

                isRunning.set(false);
            }
            Log.info("Spectator shutting down");
        }

        public void start() {
            if (isRunning.get()) Log.debug("Spectator is already running");
            Log.info("Starting spectator");
            isRunning.set(true);
            Thread.startVirtualThread(this);
        }
    }
}
