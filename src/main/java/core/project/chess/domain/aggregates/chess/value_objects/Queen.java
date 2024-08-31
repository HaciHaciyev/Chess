package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record Queen(Color color)
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

        if (!(startField.pieceOptional().get() instanceof Queen (var queenColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(queenColor);
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

        final Color opponentPieceColor = queenColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(opponentPieceColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    private StatusPair<Set<Operations>> validate(
            final ChessBoard chessBoard, final LinkedHashSet<Operations> setOfOperations, final Field startField, final Field endField
    ) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean verticalMove = startColumn == endColumn && startRow != endRow;
        if (verticalMove) {
            return clearPath
                    (chessBoard, startField.getCoordinate(), endField.getCoordinate())
                    ? StatusPair.ofTrue(setOfOperations) : StatusPair.ofFalse();
        }

        final boolean horizontalMove = startColumn != endColumn && startRow == endRow;
        if (horizontalMove) {
            return clearPath
                    (chessBoard, startField.getCoordinate(), endField.getCoordinate())
                    ? StatusPair.ofTrue(setOfOperations) : StatusPair.ofFalse();
        }

        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(columnToInt(startColumn) - columnToInt(endColumn));
        if (diagonalMove) {
            return clearPath
                    (chessBoard, startField.getCoordinate(), endField.getCoordinate())
                    ? StatusPair.ofTrue(setOfOperations) : StatusPair.ofFalse();
        }

        return StatusPair.ofFalse();
    }
}
