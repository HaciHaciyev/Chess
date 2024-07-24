package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.Operations;

public record Pawn(Color color)
        implements Piece {

    @Override
    public StatusPair<Operations> isValidMove(
            ChessBoard chessBoard, Coordinate from, Coordinate to
    ) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        /**Piece piece = from
                .piece()
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid move")
                );

        if (!(piece instanceof Pawn)) {
            throw new IllegalArgumentException("Invalid value object usage");
        }

        Pawn pawn = (Pawn) from.piece().get();

        if (pawn.color().equals(Color.BLACK)) {
            validateBlack();
        } else {
            validateWhite();
        } */

        return StatusPair.ofFalse();
    }

    private void validateBlack() {

    }

    private void validateWhite() {

    }
}
