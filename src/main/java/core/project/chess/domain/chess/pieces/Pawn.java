package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessBoard.Field;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.annotation.Nullable;

import java.util.*;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

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

        if (!(startField.pieceOptional().orElseThrow() instanceof Pawn (var pawnColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        if (!pawnColor.equals(color)) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().orElseThrow().color().equals(pawnColor);
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
        if (pawnForPromotion == null || inCaseOfPromotion == null) {
            return false;
        }

        if (inCaseOfPromotion instanceof King || inCaseOfPromotion instanceof Pawn) {
            return false;
        }

        return pawnForPromotion.color().equals(this.color) && pawnForPromotion.color().equals(inCaseOfPromotion.color());
    }

    /**
     * Determines if a pawn has no valid moves, considering the current state of the board.
     * <p>
     * This method checks all possible moves for the given pawn, including straight moves, diagonal captures,
     * passage moves, and captures on passage. If none of these moves are valid, the method returns true.
     * <p>
     * Note: The parameter {@code latestMovement} may be {@code null} only if the caller is certain that the latest move
     * was not an en passant move. Otherwise, {@code latestMovement} should always represent the last move made on the board.
     *
     * @param boardNavigator An instance of ChessBoardNavigator that provides navigation functionality for the chessboard.
     * @param ourField The field on the chessboard where the pawn is currently located.
     * @param kingColor The color of the king that the pawn belongs to.
     * @param latestMovement The latest movement made on the chessboard, represented as a pair of coordinates, or {@code null} if the last move was definitely not en passant.
     * @param pawn The pawn piece that is being checked for valid moves.
     * @return true if the pawn has no valid moves, false otherwise.
     */
    public boolean isPawnOnStalemate(final ChessBoardNavigator boardNavigator, final Field ourField,
                                     final Color kingColor, @Nullable final Pair<Coordinate, Coordinate> latestMovement,
                                     final Pawn pawn) {
        Objects.requireNonNull(boardNavigator);
        Objects.requireNonNull(ourField);
        Objects.requireNonNull(kingColor);
        Objects.requireNonNull(pawn);

        final ChessBoard chessBoard = boardNavigator.board();
        final int startColumn = ourField.getCoordinate().columnToInt();
        final int startRow = ourField.getCoordinate().getRow();
        final King king = chessBoard.theKing(kingColor);

        final List<Coordinate> fieldsForPawnMovement = boardNavigator.fieldsForPawnMovement(ourField.getCoordinate(), pawn.color());
        for (final Coordinate currentCoordinate : fieldsForPawnMovement) {
            final Field currentField = chessBoard.field(currentCoordinate);
            final boolean endFieldOccupied = currentField.isPresent();

            final boolean endFieldOccupiedByAllies = endFieldOccupied && currentField.pieceOptional().orElseThrow().color().equals(pawn.color());
            if (endFieldOccupiedByAllies) {
                continue;
            }

            final int endColumn = currentCoordinate.columnToInt();
            final int endRow = currentCoordinate.getRow();

            final boolean straightMove = startColumn == endColumn && Math.abs(startRow - endRow) == 1;
            if (straightMove && !endFieldOccupied && king.safeForKing(chessBoard, kingColor, ourField.getCoordinate(), currentCoordinate)) {
                return false;
            }

            final boolean diagonalCapture = Math.abs(startColumn - endColumn) == 1 && Math.abs(startRow - endRow) == 1;
            if (diagonalCapture && endFieldOccupied && !endFieldOccupiedByAllies &&
                    king.safeForKing(chessBoard, kingColor, ourField.getCoordinate(), currentCoordinate)) {
                return false;
            }

            final boolean isPassage = isPassage(ourField.getCoordinate(), currentCoordinate, pawn.color());
            if (isPassage && !endFieldOccupied && king.safeForKing(chessBoard, kingColor, ourField.getCoordinate(), currentCoordinate)) {

                final int passageIntermediateRow = startRow < endRow ? startRow + 1 : startRow - 1;
                final Coordinate passageIntermediateCoord = Coordinate.of(passageIntermediateRow, startColumn).orElseThrow();

                final boolean isPassageIntermediateFieldNotOccupied = chessBoard.field(passageIntermediateCoord).isEmpty();
                if (isPassageIntermediateFieldNotOccupied) {
                    return false;
                }
            }

            final boolean isCaptureOnPassage = Objects.nonNull(latestMovement) &&
                    isValidCaptureOnPassage(chessBoard, latestMovement, currentCoordinate, pawn.color());
            if (isCaptureOnPassage && !endFieldOccupied && king.safeForKing(chessBoard, kingColor, ourField.getCoordinate(), currentCoordinate)) {
                return false;
            }
        }

        return true;
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
    private StatusPair<Set<Operations>> validate(
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

        final Coordinate intermediateCoordinate = Coordinate.of(intermediateRow, column).orElseThrow();
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
        Objects.requireNonNull(chessBoard);
        if (endColumn < 1 || endColumn > 8 || endRow < 1 || endRow > 8) {
            throw new IllegalArgumentException("Illegal column or (and) row.");
        }

        Optional<Coordinate> enPassaunt = chessBoard.enPassaunt();
        if (enPassaunt.isEmpty()) {
            return false;
        }
        if (enPassaunt.get().columnToInt() != endColumn) {
            return false;
        }
        return enPassaunt.get().getRow() == endRow;
    }

    private boolean isPassage(final Coordinate coordinate, final Coordinate currentCoordinate, final Color color) {
        final int startColumn = coordinate.columnToInt();
        final int startRow = coordinate.getRow();
        final int endColumn = currentCoordinate.columnToInt();
        final int endRow = currentCoordinate.getRow();

        if (startColumn != endColumn) {
            return false;
        }

        if (Math.abs(startRow - endRow) != 2) {
            return false;
        }

        if (color.equals(WHITE)) {

            if (startRow != 2) {
                return false;
            }

            return endRow == 4;
        }

        if (startRow != 7) {
            return false;
        }

        return endRow == 5;
    }

    private boolean isValidCaptureOnPassage(final ChessBoard chessBoard,
                                            final Pair<Coordinate, Coordinate> previousPassageMove,
                                            final Coordinate endCoord,
                                            final Color color) {
        if (chessBoard.enPassaunt().isEmpty()) {
            return false;
        }

        final Coordinate coordOfPassagePawn = previousPassageMove.getSecond();

        if (color.equals(WHITE)) {
            return coordOfPassagePawn.columnToInt() == endCoord.columnToInt() && endCoord.getRow() - coordOfPassagePawn.getRow() == 1;
        } else {
            return coordOfPassagePawn.columnToInt() == endCoord.columnToInt() && endCoord.getRow() - coordOfPassagePawn.getRow() == -1;
        }
    }
}
