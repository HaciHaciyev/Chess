package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.infrastructure.utilities.containers.StatusPair;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

public final class Pawn implements Piece {
    private final Color color;
    private final int index;

    private static final Pawn WHITE_PAWN = new Pawn(WHITE, 0);
    private static final Pawn BLACK_PAWN = new Pawn(BLACK, 6);

    public static Pawn of(Color color) {
        return color == WHITE ? WHITE_PAWN : BLACK_PAWN;
    }

    private Pawn(Color color, int index) {
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

        final Set<Operations> setOfOperations = pawnMove(chessBoard, from, to, endField);
        if (setOfOperations == null) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        return setOfOperations;
    }

    public boolean isValidPromotion(final Pawn pawnForPromotion, final Piece inCaseOfPromotion) {
        if (pawnForPromotion == null || inCaseOfPromotion == null) return false;

        if (inCaseOfPromotion instanceof King || inCaseOfPromotion instanceof Pawn) return false;
        return pawnForPromotion.color() == inCaseOfPromotion.color();
    }

    public boolean isPawnOnStalemate(final ChessBoardNavigator navigator, final Coordinate pivot) {
        Objects.requireNonNull(navigator);
        Objects.requireNonNull(pivot);
        if (!navigator.board().piece(pivot).equals(this)) {
            throw new IllegalArgumentException("This object (pawn) must be located at the specified by 'pivot' coordinate.");
        }

        final ChessBoard chessBoard = navigator.board();
        final List<Coordinate> fieldsForPawnMovement = navigator.fieldsForPawnMovement(pivot, color);
        final King king = chessBoard.theKing(color);
        final int startColumn = pivot.column();
        final int startRow = pivot.row();

        for (final Coordinate endCoordinate : fieldsForPawnMovement) {
            final Piece endField = chessBoard.piece(endCoordinate);
            final boolean endFieldOccupied = endField != null;

            final boolean endFieldOccupiedByAllies = endFieldOccupied && endField.color() == color;
            if (endFieldOccupiedByAllies) continue;

            final int endColumn = endCoordinate.column();
            final int endRow = endCoordinate.row();

            final boolean straightMove = startColumn == endColumn && Math.abs(startRow - endRow) == 1;
            if (straightMove && !endFieldOccupied && king.safeForKing(chessBoard, pivot, endCoordinate)) return false;

            final boolean diagonalCapture = Math.abs(startColumn - endColumn) == 1 && Math.abs(startRow - endRow) == 1;
            if (diagonalCapture && endFieldOccupied &&
                    !endFieldOccupiedByAllies && king.safeForKing(chessBoard, pivot, endCoordinate)) return false;

            final boolean isPassage = isPassage(pivot, endCoordinate);
            if (isPassage && !endFieldOccupied && king.safeForKing(chessBoard, pivot, endCoordinate)) {
                final int passageIntermediateRow = startRow < endRow ? startRow + 1 : startRow - 1;
                final Coordinate passageIntermediateCoord = Coordinate.of(passageIntermediateRow, startColumn);

                final boolean isPassageIntermediateFieldNotOccupied = chessBoard.piece(passageIntermediateCoord) == null;
                if (isPassageIntermediateFieldNotOccupied) return false;
            }

            if (captureOnPassage(chessBoard, endColumn, endRow) && king.safeForKing(chessBoard, pivot, endCoordinate)) return false;
        }

        return true;
    }

    private Set<Operations> pawnMove(ChessBoard chessBoard, Coordinate startField, Coordinate endField, Piece endFieldPiece) {
        final int startColumn = startField.column();
        final int endColumn = endField.column();
        final int startRow = startField.row();
        final int endRow = endField.row();

        final boolean isRightPawnMovingWay = (color == WHITE && startRow < endRow) || (color == BLACK && startRow > endRow);
        if (!isRightPawnMovingWay) return null;

        final boolean straightMove = endColumn == startColumn;
        if (straightMove) return straightMove(chessBoard, startColumn, startRow, endRow, endFieldPiece);

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        if (diagonalCapture) return diagonalCapture(chessBoard, endColumn, endRow, endFieldPiece);

        return null;
    }

    private Set<Operations> straightMove(ChessBoard chessBoard, int column, int startRow, int endRow, Piece endField) {
        if (endField != null) return null;

        final boolean passage = (startRow == 7 && endRow == 5) || (startRow == 2 && endRow == 4);
        if (passage) return isPassageValid(chessBoard, column, startRow, endRow);

        Set<Operations> setOfOperations = new HashSet<>();
        final boolean validMoveDistance = Math.abs(startRow - endRow) == 1;
        final boolean fieldForPromotion = endRow == 1 || endRow == 8;

        if (fieldForPromotion && validMoveDistance) {
            setOfOperations.add(Operations.PROMOTION);
            return setOfOperations;
        }

        return validMoveDistance ? setOfOperations : null;
    }

    private Set<Operations> isPassageValid(ChessBoard chessBoard, int column, int startRow, int endRow) {
        int intermediateRow;
        if (startRow < endRow) intermediateRow = endRow - 1;
        else intermediateRow = endRow + 1;

        final Coordinate intermediateCoordinate = Coordinate.of(intermediateRow, column);
        Piece intermediateField = chessBoard.piece(intermediateCoordinate);
        if (intermediateField != null) return null;

        return new HashSet<>();
    }

    private Set<Operations> diagonalCapture(ChessBoard chessBoard, int endColumn, int endRow, Piece endField) {
        if (captureOnPassage(chessBoard, endColumn, endRow)) {
            Set<Operations> setOfOperations = new HashSet<>();
            setOfOperations.add(Operations.CAPTURE);
            return setOfOperations;
        }

        if (endField == null) return null;

        Set<Operations> setOfOperations = new HashSet<>();
        if (endRow == 1 || endRow == 8) setOfOperations.add(Operations.PROMOTION);

        setOfOperations.add(Operations.CAPTURE);
        return setOfOperations;
    }

    private boolean captureOnPassage(ChessBoard chessBoard, int endColumn, int endRow) {
        Coordinate enPassaunt = chessBoard.enPassaunt();
        if (enPassaunt == null) return false;
        if (enPassaunt.column() != endColumn) return false;
        return enPassaunt.row() == endRow;
    }

    private boolean isPassage(final Coordinate from, final Coordinate to) {
        final int startColumn = from.column();
        final int startRow = from.row();
        final int endColumn = to.column();
        final int endRow = to.row();

        if (startColumn != endColumn) return false;
        if (Math.abs(startRow - endRow) != 2) return false;
        if (color == WHITE) {
            if (startRow != 2) return false;
            return endRow == 4;
        }
        if (startRow != 7) return false;
        return endRow == 5;
    }
}
