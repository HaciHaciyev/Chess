package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.containers.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Field;
import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public record Bishop(Color color)
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

        if (!(startField.pieceOptional().orElseThrow() instanceof Bishop (var bishopColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(bishopColor);
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

        final Color opponentPieceColor = bishopColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && endField.pieceOptional().orElseThrow().color().equals(opponentPieceColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
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
    boolean validate(final ChessBoard chessBoard, final Field startField, final Field endField) {
        final int startColumn = startField.getCoordinate().columnToInt();
        final int endColumn = endField.getCoordinate().columnToInt();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(startColumn - endColumn);
        if (diagonalMove) {
            return clearPath(chessBoard, startField.getCoordinate(), endField.getCoordinate());
        }

        return false;
    }
}
