package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.Pair;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record Pawn(Color color)
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

        setOfOperations.add(influenceOnTheOpponentKing(chessBoard, from, to));

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

    private StatusPair<Set<Operations>> validate(
            final ChessBoard chessBoard, final LinkedHashSet<Operations> setOfOperations, final Field startField, final Field endField
    ) {
        final Color pawnColor = startField.pieceOptional().orElseThrow().color();
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
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

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(columnToInt(startColumn) - columnToInt(endColumn)) == 1;
        if (diagonalCapture) {
            return diagonalCapture(chessBoard, setOfOperations, startColumn, endColumn, startRow, endRow, endField);
        }

        return StatusPair.ofFalse();
    }

    private StatusPair<Set<Operations>> straightMove(
            ChessBoard chessBoard, LinkedHashSet<Operations> setOfOperations,
            char startColumn, char endColumn, int startRow, int endRow, Field endField
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
            ChessBoard chessBoard, LinkedHashSet<Operations> setOfOperations, int startRow, char column, int endRow
    ) {
        int intermediateRow;
        if (startRow < endRow) {
            intermediateRow = endRow - 1;
        } else {
            intermediateRow = endRow + 1;
        }

        final Coordinate intermediateCoordinate = Coordinate.coordinate(intermediateRow, columnToInt(column)).orElseThrow();
        Field interMediateField = chessBoard.field(intermediateCoordinate);
        if (!interMediateField.isEmpty()) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    private StatusPair<Set<Operations>> diagonalCapture(
            ChessBoard chessBoard, LinkedHashSet<Operations> setOfOperations,
            char startColumn, char endColumn, int startRow, int endRow, Field endField
    ) {

        if (Math.abs(startRow - endRow) != 1 || Math.abs(columnToInt(startColumn) - columnToInt(endColumn)) != 1) {
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

    private boolean captureOnPassage(ChessBoard chessBoard, char endColumn, int endRow) {
        Optional<Coordinate> lastMoveCoordinate = previousMoveCoordinate(chessBoard);

        return previousMoveWasPassage(chessBoard) &&
                lastMoveCoordinate.isPresent() &&
                lastMoveCoordinate.get().getColumn() == endColumn &&
                (lastMoveCoordinate.get().getRow() - endRow == 1 || lastMoveCoordinate.get().getRow() - endRow == -1);
    }

    private boolean previousMoveWasPassage(ChessBoard chessBoard) {
        if (chessBoard.latestMovement().isEmpty()) {
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
