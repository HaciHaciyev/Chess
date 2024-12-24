package core.project.chess.domain.subdomains.chess.util;

import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;
import java.util.regex.Pattern;

import static core.project.chess.domain.subdomains.chess.value_objects.AlgebraicNotation.*;

public class ChessNotationValidator {

    private ChessNotationValidator() {}

    private static final String COORDINATES = "[a-h][1-8]";

    private static final String INFLUENCE_ON_OPPONENT_KING = "[+#.]?";

    private static final String PAWN_START_MOVEMENT_COORDINATES = "[a-h][2-7]";

    /**
     * Validates a given chess move in algebraic notation.
     * <p>
     * This method checks the provided algebraic notation and validates it based on the move type:
     * <ul>
     *   <li>Simple figure movement or capture.</li>
     *   <li>Pawn movement or capture.</li>
     *   <li>Pawn promotion.</li>
     *   <li>Castling.</li>
     * </ul>
     *
     * If the notation is invalid, an {@link IllegalArgumentException} is thrown.
     *
     * @param algebraicNotation The chess move in algebraic notation to validate.
     * @throws IllegalArgumentException If the notation format is invalid.
     */
    public static void validateAlgebraicNotation(String algebraicNotation) {
        if (isSimpleFigureMovement(algebraicNotation) || isFigureCaptureOperation(algebraicNotation)) {
            validateSimpleFigureMovementOrCapture(algebraicNotation);
            return;
        }
        if (isPromotion(algebraicNotation) || isPromotionPlusOperation(algebraicNotation)) {
            validatePromotion(algebraicNotation);
            return;
        }
        if (isSimplePawnMovement(algebraicNotation)) {
            validateSimplePawnMovement(algebraicNotation);
            return;
        }
        if (isPawnCaptureOperation(algebraicNotation)) {
            validatePawnCaptureOperation(algebraicNotation);
            return;
        }
        if (isCastlePlusOperation(algebraicNotation)) {
            return;
        }

        throw new IllegalArgumentException("Invalid algebraic notation format.");
    }

    public static boolean isSimplePawnMovement(final String algebraicNotation) {
        final String regex = SIMPLE_PAWN_MOVEMENT_FORMAT.formatted(PAWN_START_MOVEMENT_COORDINATES, COORDINATES, INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isSimpleFigureMovement(final String algebraicNotation) {
        final String regex = SIMPLE_FIGURE_MOVEMENT_FORMAT.formatted("[RNBQK]", COORDINATES, COORDINATES, INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isPawnCaptureOperation(final String algebraicNotation) {
        final String regex = PAWN_CAPTURE_OPERATION_FORMAT.formatted(PAWN_START_MOVEMENT_COORDINATES, "x", COORDINATES, INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isFigureCaptureOperation(final String algebraicNotation) {
        final String regex = FIGURE_CAPTURE_OPERATION_FORMAT.formatted("[RNBQK]", COORDINATES, "x", COORDINATES, INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isCastlePlusOperation(final String algebraicNotation) {
        final String regex = CASTLE_PLUS_OPERATION_FORMAT.formatted("O-O(-O)?", INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isPromotion(final String algebraicNotation) {
        final String regex = PROMOTION_FORMAT.formatted(PAWN_START_MOVEMENT_COORDINATES, COORDINATES, "[QRBN]", INFLUENCE_ON_OPPONENT_KING);
        return Pattern.matches(regex, algebraicNotation);
    }

    public static boolean isPromotionPlusOperation(final String algebraicNotation) {
        final String regex = PROMOTION_PLUS_CAPTURE_OPERATION_FORMAT.formatted(
                PAWN_START_MOVEMENT_COORDINATES, "x", COORDINATES, "[QRBN]", INFLUENCE_ON_OPPONENT_KING
        );

        return Pattern.matches(regex, algebraicNotation);
    }

    public static void validateSimplePawnMovement(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));

        if (from.getColumn() != to.getColumn()) {
            throw new IllegalArgumentException("'From' can`t be equal to 'to' coordinate.");
        }

        final boolean validPassage = (from.getRow() == 2 && to.getRow() == 4) || (from.getRow() == 7 && to.getRow() == 5);
        if (validPassage) {
            return;
        }

        final boolean validMoveDistance = Math.abs(from.getRow() - to.getRow()) == 1;
        if (validMoveDistance) {
            final boolean fieldForPromotion = to.getRow() == 1 || to.getRow() == 8;
            if (fieldForPromotion) {
                throw new IllegalArgumentException("It is the field for PROMOTION but promotion is not added.");
            }

            return;
        }

        throw new IllegalArgumentException("Invalid pawn movement.");
    }

    public static void validatePawnCaptureOperation(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
        final int startColumn = from.columnToInt();
        final int endColumn = to.columnToInt();
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        if (diagonalCapture) {
            final boolean fieldForPromotion = to.getRow() == 1 || to.getRow() == 8;
            if (fieldForPromotion) {
                throw new IllegalArgumentException("It is the field for PROMOTION but promotion is not added.");
            }

            return;
        }

        throw new IllegalArgumentException("Invalid pawn capture operation.");
    }

    public static void validatePromotion(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
        final int startColumn = from.columnToInt();
        final int endColumn = to.columnToInt();
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean validRows = (startRow == 7 && endRow == 8) || (startRow == 2 && endRow == 1);
        if (!validRows) {
            throw new IllegalArgumentException("Invalid coordinates for pawn promotion.");
        }

        final boolean validColumns = (startColumn == endColumn) || Math.abs(startColumn - endColumn) == 1;
        if (!validColumns) {
            throw new IllegalArgumentException("Invalid coordinates for pawn promotion.");
        }
    }

    public static void validateSimpleFigureMovementOrCapture(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
        final int startColumn = from.columnToInt();
        final int endColumn = to.columnToInt();
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean king = algebraicNotation.charAt(0) == 'K';
        if (king) {
            final boolean surroundField = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;

            if (surroundField) {
                return;
            }

            throw new IllegalArgumentException("Invalid king movement.");
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

            throw new IllegalArgumentException("Invalid queen movement coordinates.");
        }

        final boolean rook = algebraicNotation.charAt(0) == 'R';
        if (rook) {
            if (verticalMove) {
                return;
            }

            if (horizontalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid rook movement coordinates.");
        }

        final boolean bishop = algebraicNotation.charAt(0) == 'B';
        if (bishop) {
            if (diagonalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid bishop movement coordinates.");
        }

        final boolean knight = algebraicNotation.charAt(0) == 'N';
        if (knight) {
            int differenceOfRow = Math.abs(from.getRow() - to.getRow());
            int differenceOfColumn = Math.abs(from.columnToInt() - to.columnToInt());
            final boolean knightMove =
                    (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);

            if (knightMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid knight movement coordinates.");
        }
    }
}
