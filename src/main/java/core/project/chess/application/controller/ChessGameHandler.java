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
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("/chess-game")
@ServerEndpoint("/chess-game/{gameId}")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JsonWebToken jwt;

    private final JWTParser jwtParser;

    private final ObjectMapper objectMapper;

    private final ChessGameService chessGameService;

    private final OutboundUserRepository outboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private static final Map<UUID, Pair<ChessGame, Set<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final Map<Username, Pair<UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    @POST @Path("/start-game") @RolesAllowed("User")
    public String startGame(@QueryParam("color") Color color, @QueryParam("type") ChessGame.TimeControllingTYPE type) {
        if (color == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Color is required.").build());
        }

        final var gameParameters = new GameParameters(
                color, Objects.requireNonNullElse(type, ChessGame.TimeControllingTYPE.DEFAULT), LocalDateTime.now()
        );

        final Username username = Result.ofThrowable(
                () -> new Username(this.jwt.getName())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build())
        );

        Log.debugf("Fetching user %s from repo", username.username());
        final UserAccount firstPlayer = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This account was not found.").build())
                );

        final StatusPair<Pair<UserAccount, GameParameters>> statusPair = findOpponent(firstPlayer, gameParameters);

        if (!statusPair.status()) {
            waitingForTheGame.put(username, Pair.of(firstPlayer, gameParameters));
            Log.infof("No opponent found for the player %s, waiting.", username);
            return "Try to find opponent for you.";
        }

        final UserAccount secondPlayer = statusPair.orElseThrow().getFirst();
        final GameParameters secondGameParameters = statusPair.orElseThrow().getSecond();
        Log.infof("Found opponent for the player %s: %s", username, secondPlayer.getUsername().username());

        waitingForTheGame.remove(secondPlayer.getUsername());
        final ChessGame chessGame = chessGameService.loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>()));

        Log.infof("Initializing Spectator for game {%s}", chessGame.getChessGameId());
        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();

        Log.debugf("Saving started game {%s}", chessGame.getChessGameId());
        inboundChessRepository.completelySaveStartedChessGame(chessGame);

        return "You partner for chess successfully founded. Starting to create session for the game {%s}.".formatted(chessGame.getChessGameId());
    }

    @OnOpen
    public void onOpen(final Session session, @PathParam("gameId") final String gameId) throws JsonProcessingException {
        if (gameId == null || gameId.isBlank()) {
            Log.error("gameId is null or blank");
            sendMessage(session, "gameId can't be null or blank");
            return;
        }

        final boolean isGameSessionExists = gameSessions.containsKey(UUID.fromString(gameId));
        if (!isGameSessionExists) {
            Log.errorf("Game session with id {%s} does not exist", gameId);
            sendMessage(session, "Game session with id {%s} does not exist".formatted(gameId));
            return;
        }

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        pair.getSecond().add(session);

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(), pair.getFirst().getChessBoard().pgn()
        );

        Log.debugf("New session for game {%s} with id {%s} is opened", gameId, session.getId());
        Log.debugf("Sending game state to session {%s}", session.getId());
        sendMessage(session, objectMapper.writeValueAsString(chessBoardMessage));
    }

    @OnMessage
    public void onMessage(final Session session, @PathParam("gameId") final String gameId, final String message) {
        if (gameId == null || gameId.isBlank()) {
            Log.error("gameId is null or blank");
            sendMessage(session, "gameId can't be null or blank");
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

        final String username = extractJWT(session).getName();
        final Pair<ChessGame, Set<Session>> gameAndSessions = gameSessions.get(UUID.fromString(gameId));

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
                                final MessageType type, final Pair<ChessGame, Set<Session>> gameAndSessions) throws JsonProcessingException {
            switch (type) {
                case MOVE -> chessGameService.move(Pair.of(username, session), jsonNode, gameAndSessions);
                case MESSAGE -> chessGameService.chat(Pair.of(username, session), jsonNode, gameAndSessions);
                case RETURN_MOVE -> chessGameService.returnOfMovement(Pair.of(username, session), gameAndSessions);
                case RESIGNATION -> chessGameService.resignation(Pair.of(username, session), gameAndSessions);
                case TREE_FOLD -> chessGameService.threeFold(Pair.of(username, session), gameAndSessions);
                case AGREEMENT -> chessGameService.agreement(Pair.of(username, session), gameAndSessions);
                default -> throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid message type.").build());
            }
    }

    @OnClose
    public void onClose(final Session session, @PathParam("gameId") final String gameId) {
        if (gameId == null || gameId.isBlank()) {
            Log.error("gameId is null or blank");
            sendMessage(session, "gameId can't be null or blank");
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
        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(gameUuid);

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

    private JsonWebToken extractJWT(final Session session) {
        final String token = session.getRequestParameterMap().get("token").getFirst();
        if (Objects.isNull(token)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        try {
            return jwtParser.parse(token);
        } catch (ParseException e) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid JWT token.").build());
        }
    }

    private StatusPair<Pair<UserAccount, GameParameters>> findOpponent(final UserAccount firstPlayer,
                                                                       final GameParameters gameParameters) {

        for (var entry : waitingForTheGame.entrySet()) {
            final UserAccount potentialOpponent = entry.getValue().getFirst();
            final GameParameters gameParametersOfPotentialOpponent = entry.getValue().getSecond();

            if (potentialOpponent.getId().equals(firstPlayer.getId())) {
                continue;
            }

            final boolean isOpponent = chessGameService.isOpponent(firstPlayer, gameParameters, potentialOpponent, gameParametersOfPotentialOpponent);
            if (isOpponent) {
                return StatusPair.ofTrue(Pair.of(potentialOpponent, gameParametersOfPotentialOpponent));
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
            this.isRunning = new AtomicBoolean();
            this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Game Spectator Thread"));
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
