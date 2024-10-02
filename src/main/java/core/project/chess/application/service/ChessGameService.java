package core.project.chess.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.ChessGameMessage;
import core.project.chess.application.dto.gamesession.ChessMovementForm;
import core.project.chess.application.dto.gamesession.GameParameters;
import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final ObjectMapper objectMapper;

    public static final double MAX_RATING_DIFF = 1500;

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    public void move(final Pair<String, Session> usernameAndSession, final JsonNode jsonNode, final Pair<ChessGame, Set<Session>> gameAndSessions)
            throws JsonProcessingException {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();
        final ChessMovementForm move = mapMessageForMovementForm(Objects.requireNonNull(jsonNode));

        Result.ofThrowable(
                () -> chessGame.makeMovement(username, move.from(), move.to(), move.inCaseOfPromotion())
        ).ifFailure(
                t -> sendMessage(usernameAndSession.getSecond(), "Invalid chess movement.")
        );

        final var message = new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(message));
        }

        if (chessGame.gameResult().isPresent()) {
            gameOverOperationsExecutor(chessGame);

            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, "Game is ended by result: {%s}.".formatted(chessGame.gameResult().get().toString()));
            }
        }
    }

    public void chat(final Pair<String, Session> usernameAndSession, final JsonNode jsonNode, final Pair<ChessGame, Set<Session>> gameAndSessions)
            throws JsonProcessingException {
        final String username = usernameAndSession.getFirst();
        final Message message = mapMessage(Objects.requireNonNull(jsonNode));

        try {
            gameAndSessions.getFirst().addChatMessage(username, message);

            for (Session session : gameAndSessions.getSecond()) {
                sendMessage(session, objectMapper.writeValueAsString(gameAndSessions.getFirst().chatMessages()));
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            sendMessage(usernameAndSession.getSecond(), "Invalid message.");
        }
    }

    public void returnOfMovement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, Set<Session>> gameAndSessions)
            throws JsonProcessingException {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        Result.ofThrowable(
                () -> chessGame.returnMovement(username)
        ).ifFailure(
                t -> sendMessage(usernameAndSession.getSecond(), "Can`t return a move.")
        );

        if (!chessGame.isLastMoveWasUndo()) {
            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, "Player {%s} requested for move returning.".formatted(username));
            }

            return;
        }

        final var message = new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, objectMapper.writeValueAsString(message));
        }
    }

    public void resignation(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, Set<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.resignation(username);

            gameOverOperationsExecutor(chessGame);

            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, "Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
            }
        } catch (IllegalArgumentException e) {
            sendMessage(usernameAndSession.getSecond(), "Not a player.");
        }
    }

    public void threeFold(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, Set<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        Result.ofThrowable(
                () -> chessGame.endGameByThreeFold(username)
        ).ifFailure(
                t -> sendMessage(usernameAndSession.getSecond(), "Can`t end game by ThreeFold")
        );

        gameOverOperationsExecutor(chessGame);

        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, "Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
        }
    }

    public void agreement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, Set<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        Result.ofThrowable(
                () -> chessGame.agreement(username)
        ).ifFailure(
                t -> sendMessage(usernameAndSession.getSecond(), "Not a player. Illegal access.")
        );

        if (!chessGame.isAgreementAvailable()) {
            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, "Player {%s} requested for agreement.".formatted(username));
            }

            return;
        }

        gameOverOperationsExecutor(chessGame);

        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, "Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString()));
        }
    }

    @Transactional
    public void gameOverOperationsExecutor(final ChessGame chessGame) {
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);
        inboundUserRepository.updateOfRating(chessGame.getPlayerForWhite());
        inboundUserRepository.updateOfRating(chessGame.getPlayerForBlack());
    }

    public boolean isOpponent(
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

        final boolean validRatingDiff = Math.abs(player.getRating().rating() - opponent.getRating().rating()) <= MAX_RATING_DIFF;
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

    public ChessGame loadChessGame(
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

        return chessGame;
    }

    private void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }

    private Message mapMessage(final JsonNode jsonNode) {
        try {
            return new Message(jsonNode.get("message").asText());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request. Invalid message.").build());
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
}
