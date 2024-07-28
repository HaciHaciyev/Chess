package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.Objects;
import java.util.Optional;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.Operations;

public record Pawn(Color color)
        implements Piece {

    @Override
    public StatusPair<Operations> isValidMove(
            final ChessBoard chessBoard, final Coordinate from, final Coordinate to
    ) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        Field startField = chessBoard.getField(from);
        Field endField = chessBoard.getField(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        final boolean isPawn = startField.pieceOptional().get() instanceof Pawn;
        if (!isPawn) {
            return StatusPair.ofFalse();
        }

        Pawn pawn = (Pawn) startField.pieceOptional().get();

        final boolean isBlack = pawn.color().equals(Color.BLACK);
        if (isBlack) {
            return validateBlack(chessBoard, startField, endField);
        }

        return validateWhite(chessBoard, startField, endField);
    }

    private StatusPair<Operations> validateBlack(
            final ChessBoard chessBoard, final Field startField, final Field endField
    ) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean endFieldEmpty = endField.isEmpty();
        final boolean endFieldOccupiedBySameColorPiece = !endFieldEmpty &&
                endField.pieceOptional().orElseThrow().color().equals(Color.BLACK);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        final boolean straightMove = endColumn == startColumn;
        if (straightMove) {
            if (!endFieldEmpty) {
                return StatusPair.ofFalse();
            }
            return blackPawnStraightMovementValidation(startRow, endRow);
        }

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 &&
                Math.abs(columnToInt(startColumn) - columnToInt(endColumn)) == 1;
        if (diagonalCapture) {
            if (captureOnPassageForBlack(chessBoard, endColumn, endRow)) {
                if (!endFieldEmpty) {
                    return StatusPair.ofFalse();
                }

                if (endRow == 1) {
                    return StatusPair.ofTrue(Operations.PROMOTION);
                }
                return StatusPair.ofTrue(Operations.CAPTURE);
            }

            if (endFieldEmpty) {
                return StatusPair.ofFalse();
            }

            if (endRow == 1) {
                return StatusPair.ofTrue(Operations.PROMOTION);
            }
            return StatusPair.ofTrue(Operations.CAPTURE);
        }

        return StatusPair.ofFalse();
    }

    private StatusPair<Operations> validateWhite(
            final ChessBoard chessBoard, final Field startField, final Field endField
    ) {
        final char startColumn = startField.getCoordinate().getColumn();
        final char endColumn = endField.getCoordinate().getColumn();
        final int startRow = startField.getCoordinate().getRow();
        final int endRow = endField.getCoordinate().getRow();

        final boolean endFieldEmpty = endField.isEmpty();
        final boolean endFieldOccupiedBySameColorFigure = !endFieldEmpty &&
                endField.pieceOptional().orElseThrow().color().equals(Color.WHITE);
        if (endFieldOccupiedBySameColorFigure) {
            return StatusPair.ofFalse();
        }

        final boolean straightMove = endColumn == startColumn;
        if (straightMove) {
            if (!endFieldEmpty) {
                return StatusPair.ofFalse();
            }
            return whitePawnStraightMovementValidation(startRow, endRow);
        }

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 &&
                Math.abs(columnToInt(startColumn) - columnToInt(endColumn)) == 1;
        if (diagonalCapture) {
            if (captureOnPassageForWhite(chessBoard, endColumn, endRow)) {
                if (!endFieldEmpty) {
                    return StatusPair.ofFalse();
                }

                if (endRow == 8) {
                    return StatusPair.ofTrue(Operations.PROMOTION);
                }
                return StatusPair.ofTrue(Operations.CAPTURE);
            }

            if (endFieldEmpty) {
                return StatusPair.ofFalse();
            }

            if (endRow == 8) {
                return StatusPair.ofTrue(Operations.PROMOTION);
            }
            return StatusPair.ofTrue(Operations.CAPTURE);
        }

        return StatusPair.ofFalse();
    }

    private int columnToInt(char startColumn) {
        return switch (startColumn) {
            case 'A' -> 1;
            case 'B' -> 2;
            case 'C' -> 3;
            case 'D' -> 4;
            case 'E' -> 5;
            case 'F' -> 6;
            case 'G' -> 7;
            case 'H' -> 8;
            default -> throw new IllegalStateException("Unexpected value: " + startColumn);
        };
    }

    private StatusPair<Operations> blackPawnStraightMovementValidation(int startRow, int endRow) {
        final boolean doubleMove = startRow == 7 && endRow == 5;
        if (doubleMove) {
            return StatusPair.ofTrue(Operations.EMPTY);
        }

        final boolean validMoveDistance = startRow - endRow == 1;

        final boolean fieldForPromotion = endRow == 1;
        if (fieldForPromotion && validMoveDistance) {
            return StatusPair.ofTrue(Operations.PROMOTION);
        }

        return validMoveDistance ? StatusPair.ofTrue(Operations.EMPTY) : StatusPair.ofFalse();
    }

    private StatusPair<Operations> whitePawnStraightMovementValidation(int startRow, int endRow) {
        final boolean doubleMove = startRow == 2 && endRow == 4;
        if (doubleMove) {
            return StatusPair.ofTrue(Operations.EMPTY);
        }

        final boolean validMoveDistance = endRow - startRow == 1;

        final boolean fieldForPromotion = endRow == 8;
        if (fieldForPromotion && validMoveDistance) {
            return StatusPair.ofTrue(Operations.PROMOTION);
        }

        return validMoveDistance ? StatusPair.ofTrue(Operations.EMPTY) : StatusPair.ofFalse();
    }

    private boolean captureOnPassageForBlack(ChessBoard chessBoard, char endColumn, int endRow) {
        final boolean previousMoveWasPassage = previousMoveWasPassage(chessBoard);

        Optional<Coordinate> lastMoveCoordinate = previousMoveCoordinate(chessBoard);
        return previousMoveWasPassage && lastMoveCoordinate.isPresent() &&
                lastMoveCoordinate.get().getColumn() == endColumn && lastMoveCoordinate.get().getRow() - endRow == 1;
    }

    private boolean captureOnPassageForWhite(ChessBoard chessBoard, char endColumn, int endRow) {
        final boolean previousMoveWasPassage = previousMoveWasPassage(chessBoard);

        Optional<Coordinate> lastMoveCoordinate = previousMoveCoordinate(chessBoard);
        return previousMoveWasPassage && lastMoveCoordinate.isPresent() &&
                lastMoveCoordinate.get().getColumn() == endColumn && lastMoveCoordinate.get().getRow() - endRow == -1;
    }

    private boolean previousMoveWasPassage(ChessBoard chessBoard) {
        if (chessBoard.getLastMove().isEmpty()) {
            return false;
        }
        AlgebraicNotation algebraicNotation = chessBoard.getLastMove().get();
        Coordinate from = algebraicNotation.getFrom();
        Coordinate to = algebraicNotation.getTo();

        final boolean pawnDoubleMove = (from.getRow() == 2 && to.getRow() == 4) || (from.getRow() == 7 && from.getRow() == 5);
        if (pawnDoubleMove) {
            return true;
        }
        return false;
    }

    private Optional<Coordinate> previousMoveCoordinate(ChessBoard chessBoard) {
        return Optional.ofNullable(
                chessBoard.getLastMove().isPresent() ? chessBoard.getLastMove().get().getTo() : null
        );
    }
}
