package core.project.chess.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.ChessMovementForm;
import core.project.chess.application.dto.gamesession.GameParameters;
import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.application.dto.gamesession.MessageType;
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
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import core.project.chess.infrastructure.utilities.json.JsonUtilities;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.infrastructure.utilities.web.WSUtilities.closeSession;
import static core.project.chess.infrastructure.utilities.web.WSUtilities.sendMessage;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final ObjectMapper objectMapper;

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> sessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Pair<ChessGame, HashSet<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, Triple<Session, UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    public void handleOnOpen(Session session, Username username) {
        Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
        if (!result.success()) {
            sendMessage(session, "This account is do not founded.");
            return;
        }

        sessions.put(username, Pair.of(session, result.value()));
    }

    public void handleOnMessage(Session session, Username username, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, "message can't be null or blank");
            return;
        }

        final boolean isGameInitialization = session.getRequestParameterMap().containsKey("game-init");
        if (isGameInitialization) {
            gameInitialization(session, username, message);
            return;
        }

        final String gameId = (String) session.getUserProperties().get("game-id");
        if (Objects.isNull(gameId)) {
            return;
        }

        final Pair<ChessGame, HashSet<Session>> gamePlusSessions = ChessGameService.gameSessions.get(UUID.fromString(gameId));
        if (Objects.isNull(gamePlusSessions)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This game session is not exits.").build());
        }

        final Result<JsonNode, Throwable> messageNode = JsonUtilities.jsonTree(message);
        if (!messageNode.success()) {
            sendMessage(session, "Invalid message.");
            return;
        }

        final Result<MessageType, Throwable> messageType = JsonUtilities.chessMessageType(message);
        if (!messageType.success()) {
            sendMessage(session, "Invalid message.");
            return;
        }

        CompletableFuture.runAsync(
                () -> handleWebSocketMessage(session, username.username(), messageNode.value(), messageType.value(), gamePlusSessions)
        );
    }

    public void handleOnClose(Session session) {
        final String gameId = (String) session.getUserProperties().get("game-id");
        if (Objects.isNull(gameId)) {
            return;
        }

        final UUID gameUuid = UUID.fromString(gameId);

        final boolean isGameSessionExists = gameSessions.containsKey(gameUuid);
        if (!isGameSessionExists) {
            Log.errorf("Game session with id {%s} does not exist", gameId);
            sendMessage(session, "Game session with id {%s} does not exist".formatted(gameId));
            return;
        }

        Log.infof("Closing websocket session for game {%s} with id {%s}", gameId, session.getId());
        final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameUuid);

        final ChessGame chessGame = pair.getFirst();
        if (chessGame.gameResult().isEmpty()) {
            return;
        }

        final Set<Session> sessionHashSet = pair.getSecond();
        final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().get().toString());
        closeSession(session, messageInCaseOfGameEnding);

        sessionHashSet.remove(session);
        if (sessionHashSet.isEmpty()) {
            ChessGameService.gameSessions.remove(gameUuid);
        }
    }

    private void gameInitialization(Session session, Username username, String message) {
        // TODO
    }

    private void handleWebSocketMessage(final Session session, final String username, final JsonNode jsonNode,
                                        final MessageType type, final Pair<ChessGame, HashSet<Session>> gameSessions) {
        switch (type) {
            case MOVE -> this.move(jsonNode, Pair.of(username, session), gameSessions);
            case MESSAGE -> this.chat(jsonNode, Pair.of(username, session), gameSessions);
            case RETURN_MOVE -> this.returnOfMovement(Pair.of(username, session), gameSessions);
            case RESIGNATION -> this.resignation(Pair.of(username, session), gameSessions);
            case TREE_FOLD -> this.threeFold(Pair.of(username, session), gameSessions);
            case AGREEMENT -> this.agreement(Pair.of(username, session), gameSessions);
            default -> sendMessage(session, "Invalid message type.");
        }
    }

    private void move(JsonNode jsonNode, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameSessions) {
        final String username = usernameSession.getFirst();
        final ChessGame chessGame = gameSessions.getFirst();
        final ChessMovementForm move = JsonUtilities.movementFormMessage(Objects.requireNonNull(jsonNode));

        try {
            chessGame.makeMovement(username, move.from(), move.to(), move.inCaseOfPromotion());
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameSession.getSecond(), "Invalid chess movement.");
            return;
        }

        final String message = JsonUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameSessions.getSecond()) {
            sendMessage(currentSession, message);
        }

        if (chessGame.gameResult().isPresent()) {
            gameOverOperationsExecutor(chessGame);

            for (Session currentSession : gameSessions.getSecond()) {
                sendMessage(currentSession, "Game is ended by result: {%s}.".formatted(chessGame.gameResult().orElseThrow().toString()));
            }
        }
    }

    public void chat(JsonNode jsonNode, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameSession.getFirst();
        final Result<Message, Throwable> message = JsonUtilities.messageRecord(jsonNode);
        if (!message.success()) {
            sendMessage(usernameSession.getSecond(), "Invalid message");
            return;
        }

        try {
            gameAndSessions.getFirst().addChatMessage(username, message.value());
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message.value().message()));

        } catch (IllegalArgumentException | NullPointerException e) {
            sendMessage(usernameSession.getSecond(), "Invalid message.");
        }
    }

    public void returnOfMovement(Pair<String, Session> usernameAndSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.returnMovement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Can`t return a move.");
            return;
        }

        if (!chessGame.isLastMoveWasUndo()) {
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, "Player {%s} requested for move returning.".formatted(username)));
            return;
        }

        final String message = JsonUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void resignation(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.resignation(username);

            gameOverOperationsExecutor(chessGame);

            String message = "Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, message);
            }
        } catch (IllegalArgumentException e) {
            sendMessage(usernameAndSession.getSecond(), "Not a player.");
        }
    }

    public void threeFold(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Can`t end game by ThreeFold");
            return;
        }

        gameOverOperationsExecutor(chessGame);

        String message = "Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void agreement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.agreement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Not a player. Illegal access.");
            return;
        }

        if (!chessGame.isAgreementAvailable()) {
            String message = "Player {%s} requested for agreement.".formatted(username);
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message));

            return;
        }

        gameOverOperationsExecutor(chessGame);

        String message = "Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    @Transactional
    public void gameOverOperationsExecutor(final ChessGame chessGame) {
        if (outboundChessRepository.isChessHistoryPresent(chessGame.getChessBoard().getChessBoardId())) {
            Log.errorf("History of game %s is already present", chessGame.getChessGameId());
            return;
        }

        Log.infof("Saving finished game %s and changing ratings", chessGame.getChessGameId());
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);
        inboundUserRepository.updateOfRating(chessGame.getPlayerForWhite());
        inboundUserRepository.updateOfRating(chessGame.getPlayerForBlack());
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

    private boolean isOpponent(
            final UserAccount player, final GameParameters gameParameters,
            final UserAccount opponent, final GameParameters opponentGameParameters
    ) {
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

    private ChessGame loadChessGame(
            final UserAccount firstPlayer, final GameParameters gameParameters, final UserAccount secondPlayer, final GameParameters secondGameParameters
    ) {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());
        final ChessGame.TimeControllingTYPE timeControlling = gameParameters.timeControllingTYPE();
        final boolean firstPlayerIsWhite = gameParameters.color() != null && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = secondGameParameters.color() != null && secondGameParameters.color().equals(Color.BLACK);

        final ChessGame chessGame;
        if (firstPlayerIsWhite && secondPlayerIsBlack) {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, firstPlayer, secondPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid data for chess game creation.").build())
            );

        } else {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, secondPlayer, firstPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid data for chess game creation.").build())
            );

        }
        Log.infof("Created chess game {%s} | Players: {%s}(%s), {%s}(%s) | Time controlling type: {%s}",
                chessGame.getChessBoard().getChessBoardId(),
                firstPlayer.getUsername().username(), firstPlayer.getRating().rating(),
                secondPlayer.getUsername().username(), secondPlayer.getRating().rating(),
                timeControlling.toString()
        );

        return chessGame;
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

                    for (Session session : gameAndSessions.getSecond()) {
                        Log.infof("Sending game result {%s} to session {%s}", gameResult, session.getId());
                        sendMessage(session, "Game is over by result {%s}".formatted(gameResult));
                    }

                    gameOverOperationsExecutor(game);
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
