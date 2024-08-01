package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();

    /** Fully validates the move, and also returns
     * a list of operations that the given operation can perform,
     * in the order in which they should be performed.*/
    StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to);

    default int columnToInt(char startColumn) {
        return switch (startColumn) {
            case 'A' -> 1;
            case 'B' -> 2;
            case 'C' -> 3;
            case 'D' -> 4;
            case 'E' -> 5;
            case 'F' -> 6;
            case 'G' -> 7;
            case 'H' -> 8;
            default -> throw new IllegalStateException("Unexpected value: " + startColumn);
        };
    }

    default Operations influenceOnTheOpponentKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        if (chessBoard.shouldPutStalemate(from, to)) {
            return Operations.STALEMATE;
        }
        if (chessBoard.shouldPutCheckmate(from, to)) {
            return Operations.CHECKMATE;
        }
        if (chessBoard.shouldPutCheck(from, to)) {
            return Operations.CHECK;
        }
        
        return Operations.EMPTY;
    }

    /**
     * Checks if the path between the given start and end coordinates on the chess board is clear.
     *
     * @param chessBoard the chess board to check the path on
     * @param from       the starting coordinate of the move
     * @param to         the ending coordinate of the move
     * @return true if the path is clear, false otherwise
     */
    default boolean clearPath(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        final int startRow = from.getRow();
        final int startColumn = columnToInt(from.getColumn());
        final int endRow = to.getRow();
        final int endColumn = columnToInt(to.getColumn());
        final int rowDirection = Integer.compare(startRow, endRow);
        final int columnDirection = Integer.compare(startColumn, endColumn);

        final boolean surroundField = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;
        if (surroundField) {
            return true;
        }

        int row = startRow + rowDirection;
        int column = startColumn + columnDirection;
        while (row < endRow && column < endColumn) {
            Coordinate coordinate = Coordinate.coordinate(row, column).valueOrElseThrow();

            final boolean fieldEmpty = chessBoard.field(coordinate).isEmpty();
            if (!fieldEmpty) {
                return false;
            }

            row += rowDirection;
            column += columnDirection;
        }

        return true;
    }
}

