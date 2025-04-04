package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;

import java.util.EnumSet;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public final class Queen implements Piece {
    private final Color color;
    private final int index;

    private static final Queen WHITE_QUEEN = new Queen(Color.WHITE, 4);
    private static final Queen BLACK_QUEEN = new Queen(Color.BLACK, 10);

    public static Queen of(Color color) {
        return color == Color.WHITE ? WHITE_QUEEN : BLACK_QUEEN;
    }

    private Queen(Color color, int index) {
        this.color = color;
        this.index = index;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece startField = chessBoard.piece(from);
        Piece endField = chessBoard.piece(to);

        if (startField == null) return null;
        final boolean endFieldOccupiedBySameColorPiece = endField != null && endField.color().equals(color);
        if (endFieldOccupiedBySameColorPiece) return null;
        if (!queenMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color().equals(opponentPieceColor);
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    /**
     * Validates whether a move from the start field to the end field on a chessboard is a valid move for a Queen.
     * <p>
     * This method checks if the move is vertical, horizontal, or diagonal. If the move is valid in any of these directions,
     * it further checks if the path between the start and end fields is clear of any pieces.
     *
     * <p>
     * Preconditions:
     * <ul>
     *     <li>The caller must ensure that the method <code>safeForKing(...)</code> has been called prior to invoking this method.
     *         This is to confirm that the move does not place the king in check.</li>
     *     <li>The caller must check that neither <code>startField</code> nor <code>endField</code> is <code>null</code>.</li>
     *     <li>The caller must verify that the <code>endField</code> is not occupied by a piece of the same color as the piece being moved.
     *         This is to ensure that the move does not violate the rules of chess regarding capturing pieces.</li>
     * </ul>
     * </p>
     *
     * @param chessBoard The chessboard on which the move is being validated. This object contains the current state of the board,
     *                   including the positions of all pieces.
     * @param startField The field from which the piece is moving. This field should contain the piece that is being moved.
     * @param endField   The field to which the piece is moving. This field is the target location for the move.
     *
     * @return <code>true</code> if the move is valid (vertical, horizontal, or diagonal) and the path is clear; <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the preconditions are not met (e.g., if <code>startField</code> or <code>endField</code> is <code>null</code>).
     */
    boolean queenMove(final ChessBoard chessBoard, final Coordinate startField, final Coordinate endField) {
        final int startColumn = startField.column();
        final int endColumn = endField.column();
        final int startRow = startField.row();
        final int endRow = endField.row();

        final boolean verticalMove = startColumn == endColumn && startRow != endRow;
        if (verticalMove) return clearPath(chessBoard, startField, endField);

        final boolean horizontalMove = startColumn != endColumn && startRow == endRow;
        if (horizontalMove) return clearPath(chessBoard, startField, endField);

        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(startColumn - endColumn);
        if (diagonalMove) return clearPath(chessBoard, startField, endField);

        return false;
    }
}
