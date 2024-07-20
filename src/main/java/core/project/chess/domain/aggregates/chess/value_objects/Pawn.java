package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;

import java.util.Objects;

public record Pawn(Color color)
        implements Piece {

    @Override
    public boolean isValidMove(
            ChessBoard chessBoard, Field currentField, Field fieldToMove
    ) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(currentField);
        Objects.requireNonNull(fieldToMove);

        Piece piece = currentField
                .piece()
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid move")
                );

        if (!(piece instanceof Pawn)) {
            throw new IllegalArgumentException("Invalid value object usage");
        }

        Pawn pawn = (Pawn) currentField.piece().get();

        if (pawn.color().equals(Color.BLACK)) {
            validateBlack();
        } else {
            validateWhite();
        }

        return true;
    }

    private void validateBlack() {

    }

    private void validateWhite() {

    }
}
