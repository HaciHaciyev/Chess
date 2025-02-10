package core.project.chess.domain.chess.factories;

import com.esotericsoftware.minlog.Log;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
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
            StatusPair<Pair<String, String>> chessNotations = isHaveChessNotations(gameParameters, secondGameParameters);
            if (!chessNotations.status()) {
                chessBoard = ChessBoard.starndardChessBoard();
            } else {
                isCasualGame = true;
                final boolean isPGN = chessNotations.orElseThrow().getFirst().equals("PGN");
                if (isPGN) {
                    chessBoard = ChessBoard.fromPGN(chessNotations.orElseThrow().getSecond());
                } else {
                    chessBoard = ChessBoard.fromPosition(chessNotations.orElseThrow().getSecond());
                }
            }
        } catch (IllegalArgumentException e) {
            Log.error("Can`t create chess game.", e.getMessage());
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

    private static StatusPair<Pair<String, String>> isHaveChessNotations(GameParameters gameParameters, GameParameters secondGameParameters) {
        if (gameParameters.PGN() != null) {
            return StatusPair.ofTrue(Pair.of("PGN", gameParameters.PGN()));
        }
        if (secondGameParameters.PGN() != null) {
            return StatusPair.ofTrue(Pair.of("PGN", secondGameParameters.PGN()));
        }
        if (gameParameters.FEN() != null) {
            return StatusPair.ofTrue(Pair.of("FEN", gameParameters.FEN()));
        }
        if (secondGameParameters.FEN() != null) {
            return StatusPair.ofTrue(Pair.of("FEN", secondGameParameters.FEN()));
        }
        return StatusPair.ofFalse();
    }
}