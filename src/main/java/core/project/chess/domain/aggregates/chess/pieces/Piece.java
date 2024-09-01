package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.StatusPair;
import java.util.Set;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();

    /**
     * Fully validates the move, and also returns
     * a list of operations that the given operation can perform,
     * in the order in which they should be performed.
     */
    StatusPair<Set<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to);

    /**
     * Determines the influence of a move on the opponent's king.
     *
     * @param chessBoard The current state of the chess board.
     * @param from       The coordinate of the piece being moved.
     * @param to         The coordinate where the piece is being moved to.
     * @return The operation that the move has on the opponent's king, which can be one of the following:
     *         - STALEMATE: The move results in a stalemate.
     *         - CHECKMATE: The move results in a checkmate.
     *         - CHECK: The move results in a check.
     *         - EMPTY: The move has no influence on the opponent's king.
     */
    default Operations influenceOnTheOpponentKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        final Color figuresColor = chessBoard.field(from).pieceOptional().orElseThrow().color();
        final Color opponentFiguresColor = figuresColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        final King opponentKing = new King(opponentFiguresColor);

        if (opponentKing.check(chessBoard, from, to)) {
            return Operations.CHECK;
        }
        if (opponentKing.checkmate(chessBoard, from, to)) {
            return Operations.CHECKMATE;
        }
        /**if (chessBoard.countOfMovement() >= 10 && opponentKing.stalemate(chessBoard, from, to)) {
            return Operations.STALEMATE;
        }*/
        
        return Operations.EMPTY;
    }

    /**
     * Checks if the path between the given start and end coordinates on the chess board is clear.
     * The method checks all fields between the start and end coordinates, but does not check the end coordinate itself.
     * If the end coordinate is a neighbor of the start coordinate, the method always returns true.
     *
     * @param chessBoard the chess board to check the path on
     * @param from       the starting coordinate of the move
     * @param to         the ending coordinate of the move
     * @return true if the path is clear (or if the end coordinate is a neighbor of the start coordinate), false otherwise
     */
    default boolean clearPath(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {

        final int startRow = from.getRow();
        final int startColumn = from.columnToInt();

        final int endRow = to.getRow();
        final int endColumn = to.columnToInt();

        final int rowDirection = compareDirection(startRow, endRow);
        final int columnDirection = compareDirection(startColumn, endColumn);

        final boolean isEndFieldSurround = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;
        if (isEndFieldSurround) {
            return true;
        }

        int row = startRow + rowDirection;
        int column = startColumn + columnDirection;

        do {
            final Coordinate coordinate = Coordinate
                    .coordinate(row, column)
                    .orElseThrow(
                            () -> new IllegalStateException("Can`t create coordinate. The method needs repair.")
                    );

            final boolean fieldEmpty = chessBoard.field(coordinate).isEmpty();
            if (!fieldEmpty) {
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

