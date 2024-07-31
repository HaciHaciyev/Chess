package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;

public record Queen(Color color)
        implements Piece {

    @Override
    public StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        Field startField = chessBoard.field(from);
        Field endField = chessBoard.field(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        if (!(startField.pieceOptional().get() instanceof Queen (var queenColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece =
                endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(queenColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();
        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }
        setOfOperations.add(influenceOnTheOpponentKing(chessBoard, from, to));

        return validate(chessBoard, setOfOperations, startField, endField);
    }

    private StatusPair<LinkedHashSet<Operations>> validate(
            final ChessBoard chessBoard, final LinkedHashSet<Operations> setOfOperations, final Field startField, final Field endField
    ) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean verticalMove = startColumn == endColumn && startRow != endRow;
        if (verticalMove) {
            return clearPath(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow);
        }

        final boolean horizontalMove = startColumn != endColumn && startRow == endRow;
        if (horizontalMove) {
            return clearPath(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow);
        }

        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(columnToInt(startColumn) - columnToInt(endColumn));
        if (diagonalMove) {
            return clearPath(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow);
        }

        return StatusPair.ofFalse();
    }

    /** TODO for me.*/
    private StatusPair<LinkedHashSet<Operations>> clearPath(
            ChessBoard chessBoard, LinkedHashSet<Operations> setOfOperations, char startColumn, char endColumn, int startRow, int endRow
    ) {
        return StatusPair.ofFalse();
    }
}
