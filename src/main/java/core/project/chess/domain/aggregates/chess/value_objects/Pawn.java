package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;

import java.util.Objects;

public record Pawn(Color color)
        implements Piece {

    @Override
    public boolean isValidMove(
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

        return true;
    }

    private void validateBlack() {

    }

    private void validateWhite() {

    }
}
