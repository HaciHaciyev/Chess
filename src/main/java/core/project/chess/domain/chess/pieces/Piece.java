package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import jakarta.annotation.Nullable;

import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();

    int index();

    /**
     * Fully validates the move, and also returns
     * a list of status that the given status need to perform.
     * Or returns null if move is invalid.
     */
    @Nullable Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to);

    /**
     * Checks if the path between the given 'start' and 'end' coordinates on the chess board is clear.
     * The method checks all fields between the 'start' and 'end' coordinates, but does not check the 'end' coordinate itself.
     * If the end coordinate is a neighbor of the start coordinate, the method always returns true.
     *
     * @param chessBoard the chess board to check the path on
     * @param from       the starting coordinate of the move
     * @param to         the ending coordinate of the move
     * @return true if the path is clear (or if the end coordinate is a neighbor of the start coordinate), false otherwise
     */
    default boolean clearPath(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {

        final int startRow = from.row();
        final int startColumn = from.column();

        final int endRow = to.row();
        final int endColumn = to.column();

        final int rowDirection = compareDirection(startRow, endRow);
        final int columnDirection = compareDirection(startColumn, endColumn);

        final boolean isEndFieldSurround = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;
        if (isEndFieldSurround) {
            return true;
        }

        int row = startRow + rowDirection;
        int column = startColumn + columnDirection;

        do {
            final Coordinate coordinate = Coordinate.of(row, column);
            final boolean fieldOccupied = chessBoard.piece(coordinate) != null;
            if (fieldOccupied) {
                return false;
            }

            row += rowDirection;
            column += columnDirection;

        } while (
                switch (rowDirection) {
                    case 0 -> true;
                    case 1 -> row < endRow;
                    case -1 -> row > endRow;
                    default -> throw new IllegalStateException("Unexpected value: " + rowDirection);
                }
                && switch (columnDirection) {
                    case 0 -> true;
                    case 1 -> column < endColumn;
                    case -1 -> column > endColumn;
                    default -> throw new IllegalStateException("Unexpected value: " + columnDirection);
                }
        );

        return true;
    }

    default int compareDirection(final int startPosition, final int endPosition) {
        if (startPosition == endPosition) {
            return 0;
        }

        return startPosition < endPosition ? 1 : -1;
    }
}

