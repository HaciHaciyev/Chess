package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;

public record Rook(Color color)
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

        if (!(startField.pieceOptional().get() instanceof Rook (var rookColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(rookColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();

        final boolean isValidMove = validate(chessBoard, startField, endField);
        if (!isValidMove) {
            return StatusPair.ofFalse();
        }

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        final Color opponentPieceColor = rookColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(opponentPieceColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    /**
     * Validates whether a move from the start field to the end field on a chessboard is a valid move for a Rook.
     * <p>
     * This method checks if the move is either vertical or horizontal. If the move is valid in either direction,
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
     * @return <code>true</code> if the move is valid (vertical or horizontal) and the path is clear; <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the preconditions are not met (e.g., if <code>startField</code> or <code>endField</code> is <code>null</code>).
     */
    boolean validate(final ChessBoard chessBoard, final Field startField, final Field endField) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean verticalMove = startColumn == endColumn && startRow != endRow;
        if (verticalMove) {
            return clearPath(chessBoard, startField.getCoordinate(), endField.getCoordinate());
        }

        final boolean horizontalMove = startColumn != endColumn && startRow == endRow;
        if (horizontalMove) {
            return clearPath(chessBoard, startField.getCoordinate(), endField.getCoordinate());
        }

        return false;
    }
}
