package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;

import java.util.regex.Pattern;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.*;

/**
 * This class provides methods to validate and process chess move notations in algebraic notation.
 * Algebraic notation is a standard way of recording and communicating chess moves, where each square on
 * the chessboard is identified by a unique combination of a letter (a-h) and a number (1-8).
 * <p>
 * The main entry point is the `validate(String algebraicNotation)` method, which takes a string representing a chess move
 * in algebraic notation and validates it according to the rules of chess.
 * If the move is valid, the method returns without throwing an exception.
 * If the move is invalid, an `IllegalArgumentException` is thrown with a descriptive error message.
 * <p>
 * The class also provides a set of helper methods that check for specific types of chess moves, such as simple pawn movements,
 * figure movements, capture operations, castling, and promotions.
 */
public class ChessNotationValidator {

    private ChessNotationValidator() {}

    public static void validate(String algebraicNotation) {
        if (isSimplePawnMovement(algebraicNotation)) {
            validateSimplePawnMovement(algebraicNotation);
            return;
        }
        if (isSimpleFigureMovement(algebraicNotation) || isFigureCaptureOperation(algebraicNotation)) {
            validateSimpleFigureMovementOrCapture(algebraicNotation);
            return;
        }
        if (isPawnCaptureOperation(algebraicNotation)) {
            validatePawnCaptureOperation(algebraicNotation);
            return;
        }
        if (isCastlePlusOperation(algebraicNotation)) {
            return;
        }
        if (isPromotion(algebraicNotation) || isPromotionPlusOperation(algebraicNotation)) {
            validatePromotion(algebraicNotation);
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid algebraic notation format: %s.", algebraicNotation));
    }

    private static boolean isSimplePawnMovement(final String algebraicNotation) {
        return Pattern.matches(String.format(SIMPLE_PAWN_MOVEMENT_FORMAT, "[A-H][1-8]", "[A-H][1-8]", "[+#.]?"), algebraicNotation);
    }

    private static boolean isSimpleFigureMovement(final String algebraicNotation) {
        return Pattern.matches(
                String.format(SIMPLE_FIGURE_MOVEMENT_FORMAT, "[RNBQK]", "[A-H][1-8]", "[A-H][1-8]", "[+#.]?"), algebraicNotation);
    }

    private static boolean isPawnCaptureOperation(final String algebraicNotation) {
        return Pattern.matches(String.format(PAWN_CAPTURE_OPERATION_FORMAT, "[A-H][1-8]", "x", "[A-H][1-8]", "[+#.]?"), algebraicNotation);
    }

    private static boolean isFigureCaptureOperation(final String algebraicNotation) {
        return Pattern.matches(
                String.format(FIGURE_CAPTURE_OPERATION_FORMAT, "[RNBQK]", "[A-H][1-8]", "x", "[A-H][1-8]", "[+#.]?"), algebraicNotation);
    }

    private static boolean isCastlePlusOperation(final String algebraicNotation) {
        return Pattern.matches(String.format(CASTLE_PLUS_OPERATION_FORMAT, "O-O(-O)?", "[+#.]?"), algebraicNotation);
    }

    private static boolean isPromotion(final String algebraicNotation) {
        return Pattern.matches(String.format(PROMOTION_FORMAT, "[A-H][1-8]", "[A-H][1-8]", "[QRBN]", "[+#.]?"), algebraicNotation);
    }

    private static boolean isPromotionPlusOperation(final String algebraicNotation) {
        return Pattern.matches(
                String.format(PROMOTION_PLUS_CAPTURE_OPERATION_FORMAT, "[A-H][1-8]", "x", "[A-h][1-8]", "[QRBN]", "[+#.]?"),
                algebraicNotation
        );
    }

    private static void validateSimplePawnMovement(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));

        if (from.getColumn() != to.getColumn()) {
            throw new IllegalStateException();
        }

        final boolean validPassage = (from.getRow() == 2 && to.getRow() == 4) || (from.getRow() == 7 && to.getRow() == 5);
        if (validPassage) {
            return;
        }

        final boolean validMoveDistance = Math.abs(from.getRow() - to.getRow()) == 1;
        if (validMoveDistance) {
            return;
        }

        throw new IllegalArgumentException("Invalid algebraic notation.");
    }


    private static void validatePawnCaptureOperation(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
        final int startColumn = columnToInt(from.getColumn());
        final int endColumn = columnToInt(to.getColumn());
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        if (diagonalCapture) {
            return;
        }

        throw new IllegalArgumentException("Invalid algebraic notation.");
    }

    private static void validatePromotion(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
        final int startColumn = columnToInt(from.getColumn());
        final int endColumn = columnToInt(to.getColumn());
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean validRows = (startRow == 7 && endRow == 8) || (startRow == 2 && endRow == 1);
        if (!validRows) {
            throw new IllegalArgumentException("Invalid algebraic notation.");
        }

        final boolean validColumns = (startColumn == endColumn) || Math.abs(startColumn - endColumn) == 1;
        if (!validColumns) {
            throw new IllegalArgumentException("Invalid algebraic notation.");
        }
    }

    private static void validateSimpleFigureMovementOrCapture(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
        final int startColumn = columnToInt(from.getColumn());
        final int endColumn = columnToInt(to.getColumn());
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean king = algebraicNotation.charAt(0) == 'K';
        if (king) {
            final boolean surroundField = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;

            if (surroundField) {
                return;
            }

            throw new IllegalArgumentException("Invalid algebraic notation.");
        }

        final boolean verticalMove = startColumn == endColumn && startRow != endRow;
        final boolean horizontalMove = startColumn != endColumn && startRow == endRow;
        final boolean diagonalMove = Math.abs(startRow - endRow) == Math.abs(startColumn - endColumn);

        final boolean queen = algebraicNotation.charAt(0) == 'Q';
        if (queen) {
            if (verticalMove) {
                return;
            }

            if (horizontalMove) {
                return;
            }

            if (diagonalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid algebraic notation.");
        }

        final boolean rook = algebraicNotation.charAt(0) == 'R';
        if (rook) {
            if (verticalMove) {
                return;
            }

            if (horizontalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid algebraic notation.");
        }

        final boolean bishop = algebraicNotation.charAt(0) == 'B';
        if (bishop) {
            if (diagonalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid algebraic notation.");
        }

        final boolean knight = algebraicNotation.charAt(0) == 'N';
        if (knight) {
            int differenceOfRow = Math.abs(from.getRow() - to.getRow());
            int differenceOfColumn = Math.abs(columnToInt(from.getColumn()) - columnToInt(to.getColumn()));
            final boolean knightMove =
                    (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);

            if (knightMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid algebraic notation.");
        }
    }
}