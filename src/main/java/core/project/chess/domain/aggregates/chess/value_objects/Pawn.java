package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.Operations;

public record Pawn(Color color)
        implements Piece {

    @Override
    public StatusPair<Operations> isValidMove(
            final ChessBoard chessBoard, final Coordinate from, final Coordinate to
    ) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        Field startField = chessBoard.getField(from);
        Field endField = chessBoard.getField(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        final boolean isPawn = startField.pieceOptional().get() instanceof Pawn;
        if (!isPawn) {
            return StatusPair.ofFalse();
        }

        Pawn pawn = (Pawn) startField.pieceOptional().get();

        final boolean isBlack = pawn.color().equals(Color.BLACK);
        if (isBlack) {
            return validateBlack(startField, endField);
        }

        return validateWhite(startField, endField);
    }

    private StatusPair<Operations> validateBlack(final Field startField, final Field endField) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean endFieldEmpty = endField.isEmpty();
        final boolean endFieldOccupiedBySameColorPiece = endFieldEmpty && endField.pieceOptional().orElseThrow().color().equals(Color.BLACK);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        final boolean straightMove = endColumn == startColumn;
        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;

        if (straightMove) {
            if (!endFieldEmpty) {
                return StatusPair.ofFalse();
            }
            return blackPawnStraightMovement(startRow, endRow);
        }

        if (diagonalCapture) {
            if (endFieldEmpty) {
                return StatusPair.ofFalse();
            }


        }

        return StatusPair.ofFalse();
    }

    private StatusPair<Operations> blackPawnStraightMovement(int startRow, int endRow) {
        final boolean doubleMove = startRow == 7 && endRow == 5;
        if (doubleMove) {
            return StatusPair.ofTrue(Operations.EMPTY);
        }

        final boolean validMoveDistance = startRow - endRow == 1;

        final boolean fieldForPromotion = endRow == 1;
        if (fieldForPromotion && validMoveDistance) {
            return StatusPair.ofTrue(Operations.PROMOTION);
        }

        return validMoveDistance ? StatusPair.ofTrue(Operations.EMPTY) : StatusPair.ofFalse();
    }

    private StatusPair<Operations> validateWhite(final Field startField, final Field endField) {
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();

        final boolean endFieldEmpty = endField.isEmpty();
        final boolean straightMove = endColumn == startColumn;
        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        final boolean endFieldOccupiedBySameColorFigure = endField.pieceOptional().orElseThrow().color().equals(Color.WHITE);

        if (endFieldOccupiedBySameColorFigure) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofFalse();
    }
}
