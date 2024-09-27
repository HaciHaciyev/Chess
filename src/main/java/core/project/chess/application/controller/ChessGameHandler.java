package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.model.ChessGameMessage;
import core.project.chess.application.model.ChessMovementForm;
import core.project.chess.application.model.GameParameters;
import core.project.chess.application.model.MessageType;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/chess-game")
@ServerEndpoint("/chess-game/{gameId}")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JsonWebToken jwt;

    private final JWTParser jwtParser;

    private final ObjectMapper objectMapper;

    private final ChessGameService chessGameService;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private static final Map<UUID, Pair<ChessGame, Set<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final Map<Username, Pair<UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    @POST @Path("/start-game") @RolesAllowed("User")
    public String startGame(@QueryParam("color") Color color, @QueryParam("type") ChessGame.TimeControllingTYPE type) {
        final var gameParameters = new GameParameters(
                color, Objects.requireNonNullElse(type, ChessGame.TimeControllingTYPE.DEFAULT), LocalDateTime.now()
        );

        final Username username = Result.ofThrowable(
                () -> new Username(this.jwt.getName())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build())
        );

        final UserAccount firstPlayer = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This account was not found.").build())
                );

        final StatusPair<Pair<UserAccount, GameParameters>> statusPair = findOpponent(firstPlayer, gameParameters);

        if (!statusPair.status()) {
            waitingForTheGame.put(username, Pair.of(firstPlayer, gameParameters));
            return "Try to find opponent for you.";
        }

        final UserAccount secondPlayer = statusPair.orElseThrow().getFirst();
        final GameParameters secondGameParameters = statusPair.orElseThrow().getSecond();

        waitingForTheGame.remove(secondPlayer.getUsername());
        final ChessGame chessGame = chessGameService.loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>()));
        inboundChessRepository.completelySaveStartedChessGame(chessGame);

        return "You partner for chess successfully founded. Starting to create session for the game {%s}.".formatted(chessGame.getChessGameId());
    }

    @OnOpen
    public void onOpen(final Session session, @PathParam("gameId") final String gameId) throws JsonProcessingException {
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);

        final boolean isGameSessionExists = gameSessions.containsKey(UUID.fromString(gameId));
        if (!isGameSessionExists) {
            return;
        }

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        pair.getSecond().add(session);

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(), pair.getFirst().getChessBoard().pgn()
        );

        sendMessage(session, objectMapper.writeValueAsString(chessBoardMessage));
    }

    @OnMessage
    public void onMessage(final Session session, @PathParam("gameId") final String gameId, final String message) throws JsonProcessingException {
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);

        final JsonNode jsonNode = getJsonTree(Objects.requireNonNull(message));
        final MessageType type = getMessageType(jsonNode);
        final String username = Objects.requireNonNull(extractJWT(session)).getName();
        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        if (Objects.isNull(pair)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This game session is not exits.").build());
        }

        switch (type) {
            case MOVE -> move(username, jsonNode, pair);
            case RETURN_MOVE -> returnOfMovement(username, pair);
            case RESIGNATION -> resignation(username, pair);
            case TREE_FOLD -> threeFold(username, pair);
            case AGREEMENT -> agreement(username, pair);
            default -> throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid message type.").build());
        }
    }

    @OnClose
    public void onClose(final Session session, @PathParam("gameId") final String gameId) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);

        final UUID gameUuid = UUID.fromString(gameId);

        final boolean isGameSessionExists = gameSessions.containsKey(gameUuid);
        if (!isGameSessionExists) {
            return;
        }

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(gameUuid);
        Objects.requireNonNull(pair);

        final ChessGame chessGame = pair.getFirst();
        if (chessGame.gameResult().isEmpty()) {
            return;
        }

        final Set<Session> sessions = pair.getSecond();
        if (!sessions.contains(session)) {
            return;
        }

        final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().get().toString());
        closeSession(session, messageInCaseOfGameEnding);

        sessions.remove(session);
        if (sessions.isEmpty()) {
            gameSessions.remove(gameUuid);
        }
    }

    private void move(final String username, final JsonNode jsonNode, final Pair<ChessGame, Set<Session>> pair) throws JsonProcessingException {
        final ChessGame chessGame = pair.getFirst();
        final ChessMovementForm move = mapMessageForMovementForm(Objects.requireNonNull(jsonNode));

        Result.ofThrowable(
                () -> chessGame.makeMovement(username, move.from(), move.to(), move.inCaseOfPromotion())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid chess movement.").build())
        );

        final var message = new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn());
        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(message));
        }

        if (chessGame.gameResult().isPresent()) {
            inboundChessRepository.completelyUpdateCompletedGame(chessGame);

            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Game is ended by result: {%s}.".formatted(chessGame.gameResult().get().toString()));
            }
        }
    }

    private void returnOfMovement(final String username, final Pair<ChessGame, Set<Session>> pair) throws JsonProcessingException {
        final ChessGame chessGame = pair.getFirst();

        Result.ofThrowable(
                () -> chessGame.returnMovement(username)
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Can`t return move.").build())
        );

        final boolean agreementNotExists =
                chessGame.getReturnOfMovement().whitePlayerUsername() == null || chessGame.getReturnOfMovement().blackPlayerUsername() == null;
        if (agreementNotExists) {
            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Player {%s} requested for move returning.".formatted(username));
            }

            return;
        }

        final var message = new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn());
        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(message));
        }
    }

    private void resignation(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        try {
            chessGame.resignation(username);

            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
            }
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Not a player.").build());
        }
    }

    private void threeFold(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        Result.ofThrowable(
                () -> chessGame.endGameByThreeFold(username)
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Can`t end game by ThreeFold rule.").build())
        );

        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, "Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
        }
    }

    private void agreement(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        Result.ofThrowable(
                () -> chessGame.agreement(username)
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Not a player in this game. Illegal access").build())
        );

        final boolean agreementNotExists =
                chessGame.getAgreementPair().whitePlayerUsername() == null || chessGame.getAgreementPair().blackPlayerUsername() == null;
        if (agreementNotExists) {
            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Player {%s} requested for agreement.".formatted(username));
            }

            return;
        }

        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, "Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
        }
    }

    private void closeSession(final Session currentSession, final String messageInCaseOfGameEnding) {
        try {
            currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, messageInCaseOfGameEnding));
        } catch (Exception e) {
            Log.info(e.getMessage());
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

    private ChessMovementForm mapMessageForMovementForm(final JsonNode node) {
        try {
            return new ChessMovementForm(
                    Coordinate.valueOf(node.get("from").asText()),
                    Coordinate.valueOf(node.get("to").asText()),
                    node.has("inCaseOfPromotion") && !node.get("inCaseOfPromotion").isNull()
                            ? AlgebraicNotation.fromSymbol(node.get("inCaseOfPromotion").asText())
                            : null
            );
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request. Invalid move format.").build());
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

}
