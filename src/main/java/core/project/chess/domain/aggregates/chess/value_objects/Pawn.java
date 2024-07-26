package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.Operations;

public record Pawn(Color color)
        implements Piece {

    @Override
    public StatusPair<Operations> isValidMove(
            ChessBoard chessBoard, Coordinate from, Coordinate to
    ) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);



        return StatusPair.ofFalse();
    }

    private void validateBlack() {

    }

    private void validateWhite() {

    }
}
