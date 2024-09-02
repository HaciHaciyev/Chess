package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.Pair;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.*;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record Pawn(Color color)
        implements Piece {

    /**
     * Validates whether a move from the 'from' coordinate to the 'to' coordinate is a valid move for a Pawn on the chessboard.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *     <li>The move is not to the same field.</li>
     *     <li>The starting field contains a piece (specifically a Pawn).</li>
     *     <li>The target field is not occupied by a piece of the same color as the Pawn.</li>
     *     <li>The move is valid according to the Pawn's movement rules.</li>
     *     <li>The move does not place the player's king in check.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Note: When calling this implementation for a Pawn, it does not validate for the promotion of the Pawn to another piece.
     * The <code>isValidMove()</code> method can account for the promotion occurring, but it cannot validate the specific piece to promote to.
     * For this, you should additionally call the function <code>isValidPromotion()</code>.
     * </p>
     *
     * @param chessBoard The chessboard on which the move is being validated. This object contains the current state of the board,
     *                   including the positions of all pieces.
     * @param from The starting coordinate of the Pawn's move. This coordinate represents the current position of the Pawn.
     * @param to   The target coordinate to which the Pawn is attempting to move. This coordinate represents the desired position.
     *
     * @return A {@link StatusPair} containing a set of operations and a boolean status indicating whether the move is valid.
     *         If the move is valid, the status will be <code>true</code> and the set will contain the operations related to the move.
     *         If the move is invalid, the status will be <code>false</code> and the set will be empty.
     *
     * @throws NullPointerException if any of the parameters (<code>chessBoard</code>, <code>from</code>, or <code>to</code>) are <code>null</code>.
     * @throws IllegalStateException if the method is called with a piece that is not a Pawn.
     */
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

        if (!(startField.pieceOptional().get() instanceof Pawn (var pawnColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(pawnColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();

        final StatusPair<Set<Operations>> isValidMove = validate(chessBoard, setOfOperations, startField, endField);
        if (!isValidMove.status()) {
            return StatusPair.ofFalse();
        }

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    public boolean isValidPromotion(final Pawn pawnForPromotion, final Piece inCaseOfPromotion) {
        Objects.requireNonNull(pawnForPromotion);
        Objects.requireNonNull(inCaseOfPromotion);

        if (inCaseOfPromotion instanceof King || inCaseOfPromotion instanceof Pawn) {
            return false;
        }

        return pawnForPromotion.color().equals(inCaseOfPromotion.color());
    }

    /**
     * Validates whether a Pawn's move from the start field to the end field on the chessboard is valid.
     *
     * <p>
     * This method checks if the Pawn is moving in the correct direction based on its color, and whether the move is either
     * a straight move or a diagonal capture. If the move is valid, it delegates the validation to the appropriate methods
     * for straight moves or diagonal captures.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> When calling this implementation for a Pawn, it does not validate for the promotion of the Pawn to another piece.
     * The <code>isValidMove()</code> method can account for the promotion occurring, but it cannot validate the specific piece to promote to.
     * For this, you should additionally call the function <code>isValidPromotion()</code>.
     * </p>
     *
     * @param chessBoard The chessboard on which the move is being validated. This object contains the current state of the board,
     *                   including the positions of all pieces.
     * @param setOfOperations A set that will be populated with operations related to the move if it is valid.
     * @param startField The field from which the Pawn is moving. This field should contain the Pawn that is being moved.
     * @param endField The field to which the Pawn is moving. This field is the target location for the move.
     *
     * @return A {@link StatusPair} indicating the result of the validation. The status will be <code>true</code> if the move is valid,
     *         and the set of operations will be populated accordingly. If the move is invalid, the status will be <code>false</code>
     *         and the set will be empty.
     *
     * @throws NoSuchElementException if the starting field does not contain a piece (the Pawn).
     */
    StatusPair<Set<Operations>> validate(
            final ChessBoard chessBoard, final Set<Operations> setOfOperations, final Field startField, final Field endField
    ) {
        final Color pawnColor = startField.pieceOptional().orElseThrow().color();
        final int startColumn = startField.getCoordinate().columnToInt();
        final int endColumn = endField.getCoordinate().columnToInt();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean isRightPawnMovingWay = (pawnColor.equals(Color.WHITE) && startRow < endRow) || (pawnColor.equals(Color.BLACK) && startRow > endRow);
        if (!isRightPawnMovingWay) {
            return StatusPair.ofFalse();
        }

        final boolean straightMove = endColumn == startColumn;
        if (straightMove) {
            return straightMove(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow, endField);
        }

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        if (diagonalCapture) {
            return diagonalCapture(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow, endField);
        }

        return StatusPair.ofFalse();
    }

    private StatusPair<Set<Operations>> straightMove(
            ChessBoard chessBoard, Set<Operations> setOfOperations,
            int startColumn, int endColumn, int startRow, int endRow, Field endField
    ) {
        if (startColumn != endColumn) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        final boolean endFieldIsEmpty = endField.isEmpty();
        if (!endFieldIsEmpty) {
            return StatusPair.ofFalse();
        }

        final boolean passage = (startRow == 7 && endRow == 5) || (startRow == 2 && endRow == 4);
        if (passage) {
            return isPassageValid(chessBoard, setOfOperations, startRow, endColumn, endRow);
        }

        final boolean validMoveDistance = Math.abs(startRow - endRow) == 1;

        final boolean fieldForPromotion = endRow == 1 || endRow == 8;
        if (fieldForPromotion && validMoveDistance) {

            setOfOperations.add(Operations.PROMOTION);
            return StatusPair.ofTrue(setOfOperations);
        }

        return validMoveDistance ? StatusPair.ofTrue(setOfOperations) : StatusPair.ofFalse();
    }

    private StatusPair<Set<Operations>> isPassageValid(
            ChessBoard chessBoard, Set<Operations> setOfOperations, int startRow, int column, int endRow
    ) {
        int intermediateRow;
        if (startRow < endRow) {
            intermediateRow = endRow - 1;
        } else {
            intermediateRow = endRow + 1;
        }

        final Coordinate intermediateCoordinate = Coordinate.coordinate(intermediateRow, column).orElseThrow();
        Field interMediateField = chessBoard.field(intermediateCoordinate);
        if (!interMediateField.isEmpty()) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    private StatusPair<Set<Operations>> diagonalCapture(
            ChessBoard chessBoard, Set<Operations> setOfOperations,
            int startColumn, int endColumn, int startRow, int endRow, Field endField
    ) {

        if (Math.abs(startRow - endRow) != 1 || Math.abs(startColumn - endColumn) != 1) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        if (captureOnPassage(chessBoard, endColumn, endRow)) {

            setOfOperations.add(Operations.CAPTURE);
            return StatusPair.ofTrue(setOfOperations);
        }

        final boolean endFieldIsEmpty = endField.isEmpty();
        if (endFieldIsEmpty) {
            return StatusPair.ofFalse();
        }

        if (endRow == 1 || endRow == 8) {
            setOfOperations.add(Operations.PROMOTION);
        }

        setOfOperations.add(Operations.CAPTURE);
        return StatusPair.ofTrue(setOfOperations);
    }

    private boolean captureOnPassage(ChessBoard chessBoard, int endColumn, int endRow) {
        Optional<Coordinate> lastMoveCoordinate = previousMoveCoordinate(chessBoard);

        return previousMoveWasPassage(chessBoard) &&
                lastMoveCoordinate.isPresent() &&
                lastMoveCoordinate.get().getColumn() == endColumn &&
                (lastMoveCoordinate.get().getRow() - endRow == 1 || lastMoveCoordinate.get().getRow() - endRow == -1);
    }

    public boolean previousMoveWasPassage(ChessBoard chessBoard) {
        if (chessBoard.latestMovement().isEmpty()) {
            return false;
        }

        final AlgebraicNotation figureType = chessBoard.lastAlgebraicNotation();
        final boolean isLastMoveWasMadeByPawn = figureType.pieceTYPE().equals(AlgebraicNotation.PieceTYPE.P);
        if (!isLastMoveWasMadeByPawn) {
            return false;
        }

        Pair<Coordinate, Coordinate> lastMovement = chessBoard.latestMovement().get();
        Coordinate from = lastMovement.getFirst();
        Coordinate to = lastMovement.getSecond();

        return (from.getRow() == 2 && to.getRow() == 4) || (from.getRow() == 7 && to.getRow() == 5);
    }

    private Optional<Coordinate> previousMoveCoordinate(ChessBoard chessBoard) {
        return Optional.ofNullable(
                chessBoard.latestMovement().isPresent() ? chessBoard.latestMovement().get().getSecond() : null
        );
    }
}
