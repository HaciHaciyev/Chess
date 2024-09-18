package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.model.ChessGameMessage;
import core.project.chess.application.model.ChessMovementForm;
import core.project.chess.application.model.GameParameters;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import io.quarkus.logging.Log;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/chess-game")
@ApplicationScoped
@RolesAllowed("USER")
@ServerEndpoint("/chess-game/{gameId}")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JsonWebToken jwt;

    private final ObjectMapper objectMapper;

    private final ChessGameService chessGameService;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private static final Map<UUID, Pair<ChessGame, Set<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final Map<Username, Pair<UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    @POST @Path("/start-game")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public String startGame(final GameParameters gameParameters) {
        Objects.requireNonNull(gameParameters);

        final Username username = new Username(jwt.getClaim("Username"));
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

        return "You partner for chess successfully founded. Starting to create session for the game.";
    }

    @OnOpen
    public void onOpen(final Session session, @PathParam("gameId") final String gameId) throws JsonProcessingException {
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
    public void onMessage(final Session session, @PathParam("gameId") final String gameId, final String message) throws JsonProcessingException {
        Objects.requireNonNull(session);
        Objects.requireNonNull(gameId);
        Objects.requireNonNull(message);

        final String username = session.getRequestParameterMap().get("token").getFirst();
        Objects.requireNonNull(username);

        final Pair<ChessGame, Set<Session>> pair = gameSessions.get(UUID.fromString(gameId));
        Objects.requireNonNull(pair);

        final ChessGame chessGame = pair.getFirst();

        final ChessMovementForm chessMovementForm = objectMapper.convertValue(message, ChessMovementForm.class);

        chessGame.makeMovement(username, chessMovementForm.from(), chessMovementForm.to(), chessMovementForm.inCaseOfPromotion());

        final ChessGameMessage chessBoardMessage = new ChessGameMessage(
                pair.getFirst().getChessBoard().actualRepresentationOfChessBoard(),
                pair.getFirst().getChessBoard().pgn()
        );

        for (Session currentSession : pair.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(chessBoardMessage));

            if (chessGame.gameResult().isPresent()) {
                final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().get().toString());

                closeSession(currentSession, messageInCaseOfGameEnding);
            }
        }

        if (chessGame.gameResult().isPresent()) {
            inboundChessRepository.completelyUpdateCompletedGame(chessGame);
            gameSessions.remove(chessGame.getChessGameId());
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
            session.getBasicRemote().sendText(message);
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
