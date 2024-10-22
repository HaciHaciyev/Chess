package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.ChessGameMessage;
import core.project.chess.application.dto.gamesession.GameParameters;
import core.project.chess.application.dto.gamesession.MessageType;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint("/chess-game")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JWTParser jwtParser;

    private final JwtUtility jwtUtility;

    private final ObjectMapper objectMapper;

    private final ChessGameService chessGameService;

    private final OutboundUserRepository outboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private static final ConcurrentHashMap<UUID, Pair<ChessGame, HashSet<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, Triple<Session, UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(final Session session) throws JsonProcessingException {
        final List<String> gameId = session.getRequestParameterMap().get("game-id");
        final boolean isGameSessionExists = gameId != null && gameSessions.containsKey(UUID.fromString(gameId.getFirst()));
        if (isGameSessionExists) {
            Log.infof("User %s trying to connect existed game with id {%s}.", gameId.getFirst(), gameId);
            connectToExistedGame(session, gameId.getFirst());
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            sendMessage(session, "Trying to create a game.");

            final var resultOfUsernameExtracting = Result.ofThrowable(() -> new Username(jwtUtility.extractJWT(session).getName()));
            final Username username = resultOfUsernameExtracting.success() ? resultOfUsernameExtracting.value() : null;
            if (Objects.isNull(username)) {
                sendMessage(session, "You do not authenticated, or you jwt token is invalid.");
                return null;
            }

            Log.infof("Fetching user %s from repository.", username.username());
            final Result<UserAccount, Throwable> foundUser = outboundUserRepository.findByUsername(username);
            final UserAccount firstPlayer = foundUser.success() ? foundUser.value() : null;
            if (Objects.isNull(firstPlayer)) {
                sendMessage(session, "You do not have a valid user account. User not found.");
                return null;
            }

            sendMessage(session, "Process for opponent finding.");
            final GameParameters gameParameters = inboundGameParameters(session);
            final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = findOpponent(firstPlayer, gameParameters);
            if (!potentialOpponent.status()) {
                waitingForTheGame.put(username, Triple.of(session, firstPlayer, gameParameters));
                Log.infof("No opponent found for the player %s, waiting.", username);
                sendMessage(session, "Try to find opponent for you %s.".formatted(username.username()));
                return null;
            }

            final Session secondSession = potentialOpponent.orElseThrow().getFirst();
            final UserAccount secondPlayer = potentialOpponent.orElseThrow().getSecond();
            final GameParameters secondGameParameters = potentialOpponent.orElseThrow().getThird();
            Log.infof("Found opponent for the player %s: %s", username, secondPlayer.getUsername().username());
            waitingForTheGame.remove(secondPlayer.getUsername());

            final ChessGame chessGame = chessGameService.loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
            final HashSet<Session> setOfSessions = new HashSet<>();
            setOfSessions.add(session);
            setOfSessions.add(secondSession);

            gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, setOfSessions));
            session.getUserProperties().put("game-id", chessGame.getChessGameId().toString());
            secondSession.getUserProperties().put("game-id", chessGame.getChessGameId().toString());

            Log.infof("Initializing Spectator for game {%s}", chessGame.getChessGameId());
            ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
            spectator.start();

            Log.debugf("Saving started game {%s}", chessGame.getChessGameId());
            inboundChessRepository.completelySaveStartedChessGame(chessGame);

            return Pair.of(secondSession, new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn()));
        }).thenAccept(pair -> {
            if (Objects.isNull(pair)) {
                return;
            }

            try {
                sendMessage(session, objectMapper.writeValueAsString(pair.getSecond()));
                sendMessage(pair.getFirst(), objectMapper.writeValueAsString(pair.getSecond()));
            } catch (JsonProcessingException e) {
                Log.error("Unexpected error occurred while sending chess board message.", e);
            }
        });
    }

    private void connectToExistedGame(final Session session, final String gameId) throws JsonProcessingException {
        final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        session.getUserProperties().put("game-id", gameId);
        pair.getSecond().add(session);

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(), pair.getFirst().getChessBoard().pgn()
        );

        Log.debugf("New session for game {%s} with id {%s} is opened", gameId, session.getId());
        Log.debugf("Sending game state to session {%s}", session.getId());
        sendMessage(session, objectMapper.writeValueAsString(chessBoardMessage));
    }

    @OnMessage
    public void onMessage(final Session session, final String message) {
        final String gameId = (String) session.getUserProperties().get("game-id");
        if (Objects.isNull(gameId)) {
            return;
        }

        if (message == null || message.isBlank()) {
            Log.error("message is null or blank");
            sendMessage(session, "message can't be null or blank");
            return;
        }

        Log.infof("Received message {%s} from session {%s} for game {%s}", message, session.getId(), gameId);

        final JsonNode jsonNode = getJsonTree(message);
        final MessageType type = getMessageType(jsonNode);

        final String username = jwtUtility.extractJWT(session).getName();
        final Pair<ChessGame, HashSet<Session>> gameAndSessions = gameSessions.get(UUID.fromString(gameId));

        if (Objects.isNull(gameAndSessions)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This game session is not exits.").build());
        }

        CompletableFuture.runAsync(() -> {
            try {
                Log.debugf("Handling %s for game {%s}", type, gameId);
                handleWebSocketMessage(session, username, jsonNode, type, gameAndSessions);
            } catch (JsonProcessingException e) {
                Log.error("Unexpected error occurred while handling websocket message.", e);
            }
        });
    }

    @Transactional
    void handleWebSocketMessage(final Session session, final String username, final JsonNode jsonNode,
                                final MessageType type, final Pair<ChessGame, HashSet<Session>> gameAndSessions) throws JsonProcessingException {
            switch (type) {
                case MOVE -> chessGameService.move(Pair.of(username, session), jsonNode, gameAndSessions);
                case MESSAGE -> chessGameService.chat(Pair.of(username, session), jsonNode, gameAndSessions);
                case RETURN_MOVE -> chessGameService.returnOfMovement(Pair.of(username, session), gameAndSessions);
                case RESIGNATION -> chessGameService.resignation(Pair.of(username, session), gameAndSessions);
                case TREE_FOLD -> chessGameService.threeFold(Pair.of(username, session), gameAndSessions);
                case AGREEMENT -> chessGameService.agreement(Pair.of(username, session), gameAndSessions);
                default -> sendMessage(session, "Invalid message type.");
            }
    }

    @OnClose
    public void onClose(final Session session) {
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

        final Set<Session> sessions = pair.getSecond();
        final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().get().toString());
        closeSession(session, messageInCaseOfGameEnding);

        sessions.remove(session);
        if (sessions.isEmpty()) {
            gameSessions.remove(gameUuid);
        }
    }

    private void closeSession(final Session currentSession, final String messageInCaseOfGameEnding) {
        try {
            currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, messageInCaseOfGameEnding));
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
    }

    private void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }

    private GameParameters inboundGameParameters(final Session session) {
        final var params = session.getRequestParameterMap();

        final Color color = Objects.nonNull(params.get("color")) ? Color.valueOf(params.get("color").getFirst()) : null;
        final var timeControl = Objects.nonNull(params.get("time-control")) ?
                ChessGame.TimeControllingTYPE.valueOf(params.get("time-control").getFirst()) :
                ChessGame.TimeControllingTYPE.DEFAULT;

        return new GameParameters(color, timeControl, LocalDateTime.now());
    }

    private JsonNode getJsonTree(final String message) {
        try {
            return objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request.").build());
        }
    }

    private MessageType getMessageType(final JsonNode node) {
        try {
            return MessageType.valueOf(node.get("type").asText());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request. Invalid Message Type.").build());
        }
    }

    private StatusPair<Triple<Session, UserAccount, GameParameters>> findOpponent(final UserAccount firstPlayer,
                                                                                  final GameParameters gameParameters) {
        Log.infof("Trying to find opponent for %s", firstPlayer);

        for (var entry : waitingForTheGame.entrySet()) {
            final UserAccount potentialOpponent = entry.getValue().getSecond();
            final GameParameters gameParametersOfPotentialOpponent = entry.getValue().getThird();

            if (potentialOpponent.getId().equals(firstPlayer.getId())) {
                continue;
            }

            final boolean isOpponent = chessGameService.isOpponent(firstPlayer, gameParameters, potentialOpponent, gameParametersOfPotentialOpponent);
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

                    chessGameService.gameOverOperationsExecutor(game);
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
