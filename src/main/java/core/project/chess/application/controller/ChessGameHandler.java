package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.model.ChessGameMessage;
import core.project.chess.application.model.ChessMovementForm;
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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

        final JsonWebToken jsonWebToken = extractJWT(session);
        final String username = Objects.requireNonNull(jsonWebToken).getName();
        final Map<String, List<String>> requestParams = session.getRequestParameterMap();
        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        if (Objects.isNull(pair)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This game session is not exits.").build());
        }

        if (requestParams.containsKey("returnOfMovementRequest")) {
            final boolean result = returnOfMovement(username, pair);
            if (result) {

                final var chessBoardMessage = new ChessGameMessage(
                        pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(),
                        pair.getFirst().getChessBoard().pgn()
                );

                for (Session currentSession : pair.getSecond()) {
                    sendMessage(currentSession, objectMapper.writeValueAsString(chessBoardMessage));
                }

                return;
            }

            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Player %s requested a return of the move.".formatted(username));
            }

            return;
        }

        if (requestParams.containsKey("resignation")) {
            resignation(username, pair);

            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Game is ended by %s player resignation.".formatted(username));
            }

            return;
        }

        if (requestParams.containsKey("threeFold")) {
            final boolean result = threeFold(username, pair);
            if (result) {
                for (Session currentSession : pair.getSecond()) {
                    sendMessage(currentSession, "Game is ended by ThreeFold rule.");
                }
            }

            return;
        }

        if (requestParams.containsKey("agreementRequest")) {
            final boolean result = agreement(username, pair);
            if (result) {
                for (Session currentSession : pair.getSecond()) {
                    sendMessage(currentSession, "Game is ended by agreement.");
                }
            }

            for (Session currentSession : pair.getSecond()) {
                sendMessage(currentSession, "Player %s requested for agreement.".formatted(username));
            }

            return;
        }

        final ChessGame chessGame = pair.getFirst();
        final ChessMovementForm move = mapMessageForMovementForm(Objects.requireNonNull(message));
        Result
                .ofThrowable(
                        () -> chessGame.makeMovement(username, move.from(), move.to(), move.inCaseOfPromotion())
                )
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid chess movement.").build())
                );

        final var chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(),
                pair.getFirst().getChessBoard().pgn()
        );

        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(chessBoardMessage));
        }

        if (chessGame.gameResult().isPresent()) {
            inboundChessRepository.completelyUpdateCompletedGame(chessGame);
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

    private boolean returnOfMovement(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        try {
            return chessGame.returnMovement(username);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Can`t return move.").build());
        }
    }

    private void resignation(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        try {
            chessGame.resignation(username);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Not a player.").build());
        }
    }

    private boolean threeFold(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        try {
            return chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Not a player. Illegal access").build());
        }
    }

    private boolean agreement(final String username, final Pair<ChessGame, Set<Session>> pair) {
        final ChessGame chessGame = pair.getFirst();

        try {
            return chessGame.agreement(username);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Not a player in this game. Illegal access").build());
        }
    }


    private ChessMovementForm mapMessageForMovementForm(final String message) {
        final JsonNode node = Result.ofThrowable(
                () -> {
                    try {
                        return objectMapper.readTree(message);
                    } catch (JsonProcessingException e) {
                        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON response.").build());
                    }
                }
        ).orElseThrow();

        try {
            return new ChessMovementForm(
                    Coordinate.valueOf(node.get("from").asText()),
                    Coordinate.valueOf(node.get("to").asText()),
                    node.has("inCaseOfPromotion") && !node.get("inCaseOfPromotion").isNull()
                            ? AlgebraicNotation.fromSymbol(node.get("inCaseOfPromotion").asText())
                            : null
            );
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request.").build());
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
