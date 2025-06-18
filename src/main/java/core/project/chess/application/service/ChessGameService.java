package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.application.dto.chess.PuzzleInbound;
import core.project.chess.application.publisher.EventPublisher;
import core.project.chess.application.requests.GameRequest;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.AgreementResult;
import core.project.chess.domain.chess.enumerations.UndoMoveResult;
import core.project.chess.domain.chess.factories.ChessGameFactory;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.services.ChessService;
import core.project.chess.domain.chess.value_objects.*;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.commons.enumerations.Color;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.value_objects.GameResult;
import core.project.chess.domain.commons.value_objects.PuzzleStatus;
import core.project.chess.domain.commons.value_objects.RatingUpdateOnPuzzle;
import core.project.chess.domain.commons.value_objects.Username;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.infrastructure.clients.PuzzlerClient;
import core.project.chess.infrastructure.dal.cache.GameInvitationsRepository;
import core.project.chess.infrastructure.dal.cache.SessionStorage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ApplicationScoped
public class ChessGameService {

    private final EventPublisher eventPublisher;

    private final ChessService chessService;

    private final PuzzlerClient puzzlerClient;

    private final SessionStorage sessionStorage;

    private final ChessGameFactory chessGameFactory;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final GameInvitationsRepository partnershipGameCacheService;

    ChessGameService(EventPublisher eventPublisher,
                     PuzzlerClient puzzlerClient,
                     SessionStorage sessionStorage,
                     ChessGameFactory chessGameFactory,
                     InboundChessRepository inboundChessRepository,
                     OutboundUserRepository outboundUserRepository,
                     ChessService gameFunctionalityService,
                     OutboundChessRepository outboundChessRepository,
                     GameInvitationsRepository partnershipGameCacheService) {

        this.eventPublisher = eventPublisher;
        this.puzzlerClient = puzzlerClient;
        this.sessionStorage = sessionStorage;
        this.chessGameFactory = chessGameFactory;
        this.inboundChessRepository = inboundChessRepository;
        this.outboundUserRepository = outboundUserRepository;
        this.chessService = gameFunctionalityService;
        this.outboundChessRepository = outboundChessRepository;
        this.partnershipGameCacheService = partnershipGameCacheService;
    }

    @WithSpan("Chess Open | SERVICE")
    public void onOpen(Session session, Username username) {
        Result<User, Throwable> result = outboundUserRepository.findByUsername(username);
        if (!result.success()) {
            String errMsg = "Account is not found";
            Span.current().addEvent(errMsg);
            Span.current().setStatus(StatusCode.ERROR);
            closeSession(session, Message.error(errMsg));
            return;
        }
        if (sessionStorage.containsSession(username)) {
            String errMsg = "You already have an active session";
            Span.current().addEvent(errMsg);
            Span.current().setStatus(StatusCode.ERROR);
            closeSession(session, Message.error(errMsg));
            return;
        }

        Span.current().addEvent("adding session to session storage");
        session.getUserProperties().put("username", username);
        session.getUserProperties().put("user-id", result.value().id());
        sessionStorage.addSession(session, result.value());
        
        sendMessage(session, Message.info("Successful connection to chessland"));
        
        Span.current().addEvent("sending invitation messages from partners");
        partnershipGameCacheService
                .getAll(username)
                .forEach((key, value) -> {
                    Message message = Message.invitation(key, value);
                    sendMessage(session, message);
                });
    }

    @WithSpan
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
        Optional<Pair<Session, User>> sessionByUsername = sessionStorage.getSessionByUsername(new Username(username));
        if (sessionByUsername.isEmpty()) {
            sendMessage(session, Message.error("Session do not exists in storage."));
            return;
        }

        UUID userID = sessionByUsername.get().getSecond().id();
        switch (message.type()) {
            case MOVE -> handleMove(session, userID, message, chessGame);
            case MESSAGE -> handleChat(session, userID, message, chessGame);
            case RETURN_MOVE -> {
                UndoMoveResult result = chessService.returnOfMovement(userID, chessGame);
                sendUndoMoveResultMessage(session, username, chessGame, result);
            }
            case RESIGNATION -> handleResignation(session, userID, chessGame);
            case TREE_FOLD -> handleThreeFold(session, userID, chessGame);
            case AGREEMENT -> {
                AgreementResult result = chessService.agreement(userID, chessGame);
                sendAgreementResultMessage(session, username, chessGame, result);
            }
            default -> sendMessage(session, Message.error("Invalid message type."));
        }
    }

    private void handleMove(Session session, UUID username, Message message, ChessGame chessGame) {
        Result<GameStateUpdate, Throwable> result = chessService.move(username, chessGame,
                message.from(), message.to(), message.inCaseOfPromotion());

        if (result.failure()) {
            sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Invalid chess movement: %s.".formatted(result.throwable().getMessage()))
                    .gameID(chessGame.chessGameID().toString())
                    .build());
            return;
        }

        GameStateUpdate update = result.value();
        sessionStorage.getGameSessions(chessGame.chessGameID())
                .forEach(gameSession -> sendMessage(gameSession, Message.gameStateUpdate(update)));
    }

    private void handleChat(Session session, UUID username, Message message, ChessGame chessGame) {
        Result<ChatMessage, Throwable> result = chessService.chat(message.message(), username, chessGame);

        if (result.failure()) {
            sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Invalid message.")
                    .gameID(chessGame.chessGameID().toString())
                    .build());
            return;
        }

        ChatMessage chatMessage = result.value();
        Message resultMessage = Message.builder(MessageType.MESSAGE)
                .gameID(chessGame.chessGameID().toString())
                .message(chatMessage.message())
                .build();

        sessionStorage.getGameSessions(chessGame.chessGameID()).stream()
                .filter(gameSession -> chessGame.isPlayer(extractUserID(gameSession)))
                .forEach(gameSession -> sendMessage(gameSession, resultMessage));
    }

    private void handleResignation(Session session, UUID userID, ChessGame chessGame) {
        Result<GameResult, Throwable> result = chessService.resignation(userID, chessGame);

        if (result.failure()) {
            sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Not a player.")
                    .gameID(chessGame.chessGameID().toString())
                    .build());
            return;
        }

        Message resultMessage = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.chessGameID().toString())
                .message("Game is ended by result {%s}.".formatted(chessGame.gameResult().toString()))
                .build();

        sessionStorage.getGameSessions(chessGame.chessGameID())
                .forEach(gameSession -> sendMessage(gameSession, resultMessage));
    }

    private void handleThreeFold(Session session, UUID username, ChessGame chessGame) {
        boolean gameEnded = chessService.threeFold(username, chessGame);

        if (!gameEnded) {
            sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Can`t end game by ThreeFold.")
                    .gameID(chessGame.chessGameID().toString())
                    .build());
            return;
        }

        Message resultMessage = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.chessGameID().toString())
                .message("Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().toString()))
                .build();

        sessionStorage.getGameSessions(chessGame.chessGameID())
                .forEach(gameSession -> sendMessage(gameSession, resultMessage));
    }

    private void sendUndoMoveResultMessage(Session session, String username,
                                           ChessGame chessGame, UndoMoveResult result) {
        switch (result) {
            case SUCCESSFUL_UNDO -> {
                Message message = Message.builder(MessageType.FEN_PGN)
                        .gameID(chessGame.chessGameID().toString())
                        .FEN(chessGame.fen())
                        .PGN(chessGame.pgn())
                        .timeLeft(chessService.remainingTimeAsString(chessGame))
                        .isThreeFoldActive(chessGame.isThreeFoldActive())
                        .build();

                sessionStorage.getGameSessions(chessGame.chessGameID())
                        .forEach(gameSession -> sendMessage(gameSession, message));
            }
            case UNDO_REQUESTED -> {
                Message message = Message.builder(MessageType.RETURN_MOVE)
                        .message("Player {%s} requested for move returning.".formatted(username))
                        .gameID(chessGame.chessGameID().toString())
                        .build();

                sessionStorage.getGameSessions(chessGame.chessGameID())
                        .forEach(gameSession -> sendMessage(gameSession, message));
            }
            case FAILED_UNDO -> sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Can`t return a move.")
                    .gameID(chessGame.chessGameID().toString())
                    .build());
        }
    }

    private void sendAgreementResultMessage(Session session, String username,
                                            ChessGame chessGame, AgreementResult result) {
        switch (result) {
            case AGREED -> {
                Message message = Message.builder(MessageType.GAME_ENDED)
                        .gameID(chessGame.chessGameID().toString())
                        .message("Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().name()))
                        .build();

                sessionStorage.getGameSessions(chessGame.chessGameID())
                        .forEach(gameSession -> sendMessage(gameSession, message));
            }
            case REQUESTED -> {
                Message message = Message.builder(MessageType.AGREEMENT)
                        .gameID(chessGame.chessGameID().toString())
                        .message("Player {%s} requested for agreement.".formatted(username))
                        .build();

                sessionStorage.getGameSessions(chessGame.chessGameID())
                        .forEach(gameSession -> sendMessage(gameSession, message));
            }
            case FAILED -> sendMessage(session, Message.builder(MessageType.ERROR)
                    .message("Not a player. Illegal access.")
                    .gameID(chessGame.chessGameID().toString())
                    .build());
        }
    }

    private void handlePuzzleAction(Session session, Username username, Message message) {
        User user = sessionStorage.getSessionByUsername(username).orElseThrow().getSecond();

        if (message.type().equals(MessageType.PUZZLE)) {
            Puzzle puzzle = chessPuzzle(user);
            sessionStorage.addPuzzle(puzzle, username);
            sendMessage(session, Message.builder(MessageType.PUZZLE)
                    .gameID(puzzle.id().toString())
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

        Result<PuzzleStateUpdate, Throwable> response = chessService.puzzleMove(
                puzzle.get(),
                message.from(),
                message.to(),
                message.inCaseOfPromotion()
        );

        if (response.failure()) sendMessage(session, Message.error("Invalid puzzle move."));

        PuzzleStateUpdate stateUpdate = response.value();

        sendMessage(session, Message.builder(MessageType.PUZZLE_MOVE)
                .gameID(stateUpdate.gameID().toString())
                .FEN(stateUpdate.fen())
                .PGN(stateUpdate.pgn())
                .isPuzzleEnded(stateUpdate.isPuzzleEnded())
                .isPuzzleSolved(stateUpdate.isPuzzleSolved())
                .build());

        updatePuzzleRatingAtTheEnd(puzzle.get(), user);
    }

    private void updatePuzzleRatingAtTheEnd(Puzzle puzzle, User user) {
        if (!puzzle.isEnded()) return;

        var ratingUpdateOnPuzzle = new RatingUpdateOnPuzzle(puzzle.id(), user.id(), puzzle.rating(), user.puzzlesRating(),
                puzzle.isSolved() ? PuzzleStatus.SOLVED : PuzzleStatus.UNSOLVED);
        puzzle.changeRating(ratingUpdateOnPuzzle);

        inboundChessRepository.updatePuzzleOnSolving(puzzle);
        eventPublisher.publishAllPuzzle(puzzle.pullDomainEvents());
    }

    private Puzzle chessPuzzle(User user) {
        PuzzleRatingWindow ratingWindow = new PuzzleRatingWindow(user.puzzlesRating().rating());
        var puzzleProperties = outboundChessRepository.puzzle(user.id(), ratingWindow).orElseThrow();

        return Puzzle.fromRepository(
                puzzleProperties.puzzleId(),
                user.id(), puzzleProperties.PGN(),
                puzzleProperties.startPosition(),
                puzzleProperties.rating()
        );
    }

    private void createPuzzle(List<PuzzleInbound.Move> moves, int startPositionOfPuzzle) {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        try {
            for (PuzzleInbound.Move move : moves) chessBoard.doMove(move.from(), move.to(), getPromotion(move, chessBoard));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid puzzle provided.");
        }

        Puzzle puzzle = Puzzle.of(chessBoard.pgn(), startPositionOfPuzzle);
        inboundChessRepository.savePuzzle(puzzle);
    }

    private static Piece getPromotion(PuzzleInbound.Move move, ChessBoard chessBoard) {
        return move.promotion() == null ? null : switch (move.promotion()) {
            case q -> Queen.of(chessBoard.turn());
            case r -> Rook.of(chessBoard.turn());
            case b -> Bishop.of(chessBoard.turn());
            case n -> Knight.of(chessBoard.turn());
        };
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
        UUID userID = extractUserID(session);
        if (game.isPlayer(userID)) {
            game.returnedToTheBoard(userID);
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

                final boolean isOpponent = chessService.validateOpponentEligibility(
                        Pair.of(firstPlayer.id(), firstPlayer.ratings()),
                        gameParameters,
                        Pair.of(potentialOpponent.id(), potentialOpponent.ratings()),
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

        final boolean isOpponentEligible = chessService.validateOpponentEligibility(
                Pair.of(addresserAccount.id(), addresserAccount.ratings()), addresserGameParameters,
                Pair.of(addresseeAccount.id(), addresseeAccount.ratings()), addresseeGameParameters,
                true
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

        Result<ChessGame, Throwable> chessGame = chessGameFactory.createChessGameInstance(
                Pair.of(firstPlayer.id(), firstPlayer.ratings()),
                firstGameParameters,
                Pair.of(secondPlayer.id(), secondPlayer.ratings()),
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

    private static UUID extractUserID(Session session) {
        Object o = session.getUserProperties().get("user-id");
        String userID = (String) o;
        return UUID.fromString(userID);
    }

    private void sendGameStartNotifications(Session session, ChessGame chessGame) {
        final Message overviewMessage = Message.builder(MessageType.GAME_START_INFO)
                .gameID(chessGame.chessGameID().toString())
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

        final UUID userID = extractUserID(session);
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
                if (chessGame.get().isPlayer(userID)) handleAFK(userID, chessGame.get(), gameUuid);
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

    private void handleAFK(UUID username, ChessGame chessGame, UUID gameUuid) {
        chessGame.awayFromTheBoard(username);
        Message message = Message.builder(MessageType.INFO)
                .gameID(chessGame.chessGameID().toString())
                .message("Player %s is AFK.")
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
                chessService.executeGameOverOperations(game);
                eventPublisher.publishAllChessGame(game.pullDomainEvents());
                puzzlerClient.sendPGN(game.pgn(), res -> {
                    var puzzle = res.body();
                    Log.info("Got puzzle: " + puzzle);
                    createPuzzle(puzzle.moves(), puzzle.startPositionOfPuzzle());
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
