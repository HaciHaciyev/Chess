package core.project.chess.application.service;

import core.project.chess.application.model.GameParameters;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
public class ChessGameService {

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
}
