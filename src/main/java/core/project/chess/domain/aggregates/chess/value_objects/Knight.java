package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;

public record Knight(Color color)
        implements Piece {

    @Override
    public StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
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

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();
        setOfOperations.add(influenceOnTheOpponentKing(chessBoard, from, to));

        final Color opponentPieceColor = knightColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(opponentPieceColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        final boolean knightMove = knightMove(startField.getCoordinate(), endField.getCoordinate());
        if (knightMove) {
            return StatusPair.ofTrue(setOfOperations);
        }

        return StatusPair.ofFalse();
    }

    private boolean knightMove(final Coordinate from, final Coordinate to) {
        int differenceOfRow = Math.abs(from.getRow() - to.getRow());
        int differenceOfColumn = Math.abs(columnToInt(from.getColumn()) - columnToInt(to.getColumn()));

        return (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);
    }
}
