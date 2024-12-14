package core.project.chess.domain.subdomains.chess.pieces;

import core.project.chess.domain.subdomains.chess.entities.ChessBoard;
import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.containers.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.subdomains.chess.entities.ChessBoard.Field;
import static core.project.chess.domain.subdomains.chess.entities.ChessBoard.Operations;

public record Knight(Color color)
        implements Piece {

    @Override
    public StatusPair<Set<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) {
            return StatusPair.ofFalse();
        }

        Field startField = chessBoard.field(from);
        Field endField = chessBoard.field(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        if (!(startField.pieceOptional().get() instanceof Knight (var knightColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(knightColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        final boolean knightMove = knightMove(startField.getCoordinate(), endField.getCoordinate());
        if (!knightMove) {
            return StatusPair.ofFalse();
        }

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();

        final Color opponentPieceColor = knightColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(opponentPieceColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
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
        int differenceOfRow = Math.abs(from.getRow() - to.getRow());
        int differenceOfColumn = Math.abs(from.columnToInt() - to.columnToInt());

        return (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);
    }
}
