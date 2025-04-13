package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
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
    @Nullable
    Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to);

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
        if (isEndFieldSurround) return true;

        int row = startRow + rowDirection;
        int column = startColumn + columnDirection;

        do {
            final Coordinate coordinate = Coordinate.of(row, column);
            final boolean fieldOccupied = chessBoard.piece(coordinate) != null;
            if (fieldOccupied) return false;

            row += rowDirection;
            column += columnDirection;

        } while (
                switch (rowDirection) {
                    case 0 -> true;
                    case 1 -> row < endRow;
                    case -1 -> row > endRow;
                    default -> throw new IllegalStateException("Unexpected value: " + rowDirection);
                } &&
                switch (columnDirection) {
                    case 0 -> true;
                    case 1 -> column < endColumn;
                    case -1 -> column > endColumn;
                    default -> throw new IllegalStateException("Unexpected value: " + columnDirection);
                }
        );

        return true;
    }

    default boolean clearPath(
            final ChessBoard chessBoard,
            final Coordinate from,
            final Coordinate to,
            final Coordinate ignore) {

        final int startRow = from.row();
        final int startColumn = from.column();

        final int endRow = to.row();
        final int endColumn = to.column();

        final int rowDirection = compareDirection(startRow, endRow);
        final int columnDirection = compareDirection(startColumn, endColumn);

        final boolean isEndFieldSurround = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;
        if (isEndFieldSurround) return true;

        int row = startRow + rowDirection;
        int column = startColumn + columnDirection;

        do {
            final Coordinate coordinate = Coordinate.of(row, column);
            final boolean fieldOccupied = chessBoard.piece(coordinate) != null;
            if (fieldOccupied && coordinate != ignore) return false;

            row += rowDirection;
            column += columnDirection;

        } while (
                switch (rowDirection) {
                    case 0 -> true;
                    case 1 -> row < endRow;
                    case -1 -> row > endRow;
                    default -> throw new IllegalStateException("Unexpected value: " + rowDirection);
                } &&
                switch (columnDirection) {
                    case 0 -> true;
                    case 1 -> column < endColumn;
                    case -1 -> column > endColumn;
                    default -> throw new IllegalStateException("Unexpected value: " + columnDirection);
                }
        );

        return true;
    }

    default int compareDirection(final int startPosition, final int endPosition) {
        if (startPosition == endPosition) return 0;
        return startPosition < endPosition ? 1 : -1;
    }

    @Nullable
    default Coordinate occupiedFieldInDirection(ChessBoard chessBoard, Direction direction, Coordinate pivot) {
        long occupied = chessBoard.whitePieces() | chessBoard.blackPieces();
        int fromIndex = pivot.ordinal();

        long ray = rayMask(direction, fromIndex);
        long rayOccupied = ray & occupied;

        if (rayOccupied == 0) return null;

        int index;
        if (direction.isTowardsLowBits()) {
            index = Long.numberOfTrailingZeros(rayOccupied);
        } else {
            index = 63 - Long.numberOfLeadingZeros(rayOccupied);
        }

        return Coordinate.byOrdinal(index);
    }

    @Nullable
    default Coordinate findXRayAttackerInDirection(
            Direction direction,
            Coordinate pivot,
            Coordinate from,
            long simulatedBitboard,
            long simulatedOpponentBitboard) {

        Coordinate current = pivot;
        while (true) {
            current = direction.apply(current);
            if (current == null) return null;

            long bit = current.bitMask();
            if ((simulatedBitboard & bit) != 0) {
                if ((simulatedOpponentBitboard & bit) != 0) return current;
                return null;
            }
        }
    }

    default List<Coordinate> surroundingFields(Coordinate pivot) {
        long surroundingsBitboard = surroundingFieldBitboard(pivot);

        List<Coordinate> surroundings = new ArrayList<>(8);
        while (surroundingsBitboard != 0) {
            int coordinateIndex = Long.numberOfTrailingZeros(surroundingsBitboard);
            surroundingsBitboard &= surroundingsBitboard - 1;
            surroundings.add(Coordinate.byOrdinal(coordinateIndex));
        }

        return surroundings;
    }

    private static long surroundingFieldBitboard(Coordinate pivot) {
        int square = pivot.ordinal();
        long moves = King.WHITE_KING_MOVES_CACHE[square];
        if (square == 60) {
            moves &= ~(1L << 62);
            moves &= ~(1L << 58);
        }
        if (square == 4) {
            moves &= ~(1L << 6);
            moves &= ~(1L << 2);
        }
        return moves;
    }

    default long rayMask(Direction direction, int fromSquare) {
        long ray = 0L;
        int row = fromSquare / 8;
        int col = fromSquare % 8;

        int rowStep = -direction.rowDelta();
        int colStep = direction.colDelta();

        row += rowStep;
        col += colStep;

        while (row >= 0 && row < 8 && col >= 0 && col < 8) {
            int index = row * 8 + col;
            ray |= (1L << index);

            row += rowStep;
            col += colStep;
        }

        return ray;
    }

    default List<Coordinate> fieldsInPath(Coordinate start, Coordinate end) {
        Direction direction = Direction.ofPath(start, end);
        List<Coordinate> fields = new ArrayList<>();

        Coordinate current = start;
        while (true) {
            Coordinate next = direction.apply(current);
            if (next == null) break;

            current = next;

            if (current.equals(end)) break;
            fields.add(current);
        }

        return fields;
    }
}

