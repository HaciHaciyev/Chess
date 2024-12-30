package core.project.chess.domain.chess.factories;

import com.esotericsoftware.minlog.Log;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ChessGameFactory {

    public Result<ChessGame, IllegalArgumentException> createChessGameInstance(final UserAccount firstPlayer, final GameParameters gameParameters,
                                                                               final UserAccount secondPlayer, final GameParameters secondGameParameters) {
        final ChessBoard chessBoard;
        boolean isCasualGame = gameParameters.isCasualGame();
        try {
            if (Objects.isNull(gameParameters.FEN()) && Objects.isNull(gameParameters.PGN())) {
                chessBoard = ChessBoard.starndardChessBoard();
            } else {
                isCasualGame = true;
                if (Objects.nonNull(gameParameters.PGN())) {
                    chessBoard = ChessBoard.fromPGN(gameParameters.PGN());
                } else {
                    chessBoard = ChessBoard.fromPosition(gameParameters.FEN());
                }
            }
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }

        final ChessGame.Time timeControlling = gameParameters.time();
        final boolean firstPlayerIsWhite = Objects.nonNull(gameParameters.color()) && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = Objects.nonNull(secondGameParameters.color()) && secondGameParameters.color().equals(Color.BLACK);

        if (firstPlayerIsWhite && secondPlayerIsBlack) {
            ChessGame chessGame = ChessGame.of(
                    UUID.randomUUID(),
                    chessBoard,
                    firstPlayer,
                    secondPlayer,
                    SessionEvents.defaultEvents(),
                    timeControlling,
                    isCasualGame
            );
            return Result.success(chessGame);
        }

        ChessGame chessGame = ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                secondPlayer,
                firstPlayer,
                SessionEvents.defaultEvents(),
                timeControlling,
                isCasualGame
        );

        return Result.success(chessGame);
    }
}