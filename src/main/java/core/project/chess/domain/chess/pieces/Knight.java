package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public record Knight(Color color) implements Piece {

    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.equals(to)) return null;

        Piece startField = chessBoard.piece(from);
        Piece endField = chessBoard.piece(to);

        if (startField == null) return null;
        final boolean endFieldOccupiedBySameColorPiece = endField != null && endField.color().equals(color);
        if (endFieldOccupiedBySameColorPiece) return null;
        if (!knightMove(from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = new HashSet<>();

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color().equals(opponentPieceColor);
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    /**
     * Validates whether a move from the 'from' coordinate to the 'to' coordinate is a valid knight move in chess.
     * <p>
     * A knight moves in an "L" shape: it can move two squares in one direction (either horizontally or vertically)
     * and then one square in a perpendicular direction, or one square in one direction and then two squares in a
     * perpendicular direction. This method checks if the given coordinates represent such a move.
     *
     * <p>
     * Preconditions:
     * <ul>
     *     <li>The caller must ensure that the method <code>safeForKing(...)</code> has been called prior to invoking this method.
     *         This is to confirm that the move does not place the king in check.</li>
     *     <li>The caller must check that neither <code>from</code> nor <code>to</code> is <code>null</code>.</li>
     *     <li>The caller must verify that the <code>to</code> coordinate is not occupied by a piece of the same color as the knight.
     *         This is to ensure that the move does not violate the rules of chess regarding capturing pieces.</li>
     * </ul>
     * </p>
     *
     * @param from The starting coordinate of the knight's move. This coordinate represents the current position of the knight.
     * @param to   The target coordinate to which the knight is attempting to move. This coordinate represents the desired position.
     *
     * @return <code>true</code> if the move from 'from' to 'to' is a valid knight move; <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the preconditions are not met (e.g., if <code>from</code> or <code>to</code> is <code>null</code>).
     */
    boolean knightMove(final Coordinate from, final Coordinate to) {
        int differenceOfRow = Math.abs(from.row() - to.row());
        int differenceOfColumn = Math.abs(from.column() - to.column());

        return (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);
    }
}
