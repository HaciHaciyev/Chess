package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public final class Bishop implements Piece {
    private final Color color;
    private final int index;

    private static final Bishop WHITE_BISHOP = new Bishop(Color.WHITE, 2);
    private static final Bishop BLACK_BISHOP = new Bishop(Color.BLACK, 8);

    public static Bishop of(Color color) {
        return color == Color.WHITE ? WHITE_BISHOP : BLACK_BISHOP;
    }

    private Bishop(Color color, int index) {
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
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.equals(to)) {
            return null;
        }

        Piece startField = chessBoard.piece(from);
        Piece endField = chessBoard.piece(to);

        if (startField == null) return null;
        final boolean endFieldOccupiedBySameColorPiece = endField != null && endField.color().equals(color);
        if (endFieldOccupiedBySameColorPiece) return null;
        if (!bishopMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = new HashSet<>();

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color().equals(opponentPieceColor);
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    /**
     * Validates whether a move from the start field to the end field on a chessboard is a valid diagonal move.
     * <p>
     * This method checks if the move is diagonal by comparing the row and column coordinates of the start and end fields.
     * If the move is diagonal, it further checks if the path between the start and end fields is clear of any pieces.
     *
     * <p>
     * Preconditions:
     * <ul>
     *     <li>The caller must ensure that the method <code>safeForKing(...)</code> has been called prior to invoking this method.
     *         This is to confirm that the move does not place the king in check.</li>
     *     <li>The caller must check that neither <code>chessBoard</code> nor <code>startField</code> nor <code>endField</code> is <code>null</code>.</li>
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
     * @return <code>true</code> if the move is a valid diagonal move and the path is clear; <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the preconditions are not met (e.g., if <code>startField</code> or <code>endField</code> is <code>null</code>).
     */
    boolean bishopMove(final ChessBoard chessBoard, final Coordinate startField, final Coordinate endField) {
        final int startColumn = startField.column();
        final int endColumn = endField.column();
        final int startRow = startField.row();
        final int endRow = endField.row();

        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(startColumn - endColumn);
        if (diagonalMove) return clearPath(chessBoard, startField, endField);

        return false;
    }
}
