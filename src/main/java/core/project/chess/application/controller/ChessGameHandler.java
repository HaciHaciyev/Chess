package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.model.ChessGameMessage;
import core.project.chess.application.model.GameParameters;
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
        Log.info("Create a game.");

        final var gameParameters = new GameParameters(
                color, Objects.requireNonNullElse(type, ChessGame.TimeControllingTYPE.DEFAULT), LocalDateTime.now()
        );

        final Username username = new Username(this.jwt.getName());
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
        Log.info("Open websocket session.");
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        pair.getSecond().add(session);

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(), pair.getFirst().getChessBoard().pgn()
        );

        sendMessage(session, objectMapper.writeValueAsString(chessBoardMessage));
    }

    @OnMessage
    public void onMessage(final Session session, @PathParam("gameId") final String gameId, final String message)
            throws JsonProcessingException, ParseException {
        Log.info("Handle message.");
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);
        Objects.requireNonNull(message);

        final String token = session.getRequestParameterMap().get("token").getFirst();
        final JsonWebToken jsonWebToken = jwtParser.parse(token);

        final String username = jsonWebToken.getName();
        Objects.requireNonNull(username);

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        Objects.requireNonNull(pair);

        final ChessGame chessGame = pair.getFirst();

        final JsonNode node = objectMapper.readTree(message);
        chessGame.makeMovement(
                username,
                Coordinate.valueOf(node.get("from").asText()),
                Coordinate.valueOf(node.get("to").asText()),
                node.has("inCaseOfPromotion") && !node.get("inCaseOfPromotion").isNull()
                        ? AlgebraicNotation.fromSymbol(node.get("inCaseOfPromotion").asText())
                        : null
        );

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(),
                pair.getFirst().getChessBoard().pgn()
        );

        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(chessBoardMessage));
        }
    }

    @OnClose
    public void onClose(final Session session, @PathParam("gameId") final String gameId) {
        Log.info("Handle close.");
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);

        final UUID gameUuid = UUID.fromString(gameId);

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
