package core.project.chess.domain.subdomains.chess.services;

import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;
import core.project.chess.domain.subdomains.chess.value_objects.FromFEN;
import core.project.chess.infrastructure.utilities.containers.StatusPair;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static core.project.chess.domain.subdomains.chess.entities.AlgebraicNotation.*;

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
 *
 * @author Hadzhyiev Hadzhy
 */
public class ChessNotationsValidator {

    private ChessNotationsValidator() {}

    private static final String COORDINATES = "[a-h][1-8]";

    private static final String INFLUENCE_ON_OPPONENT_KING = "[+#.]?";

    private static final String PAWN_START_MOVEMENT_COORDINATES = "[a-h][2-7]";

    private static final Pattern passageCoordinatePattern = Pattern.compile("\\b[a-h][36]\\b");

    private static final String FEN_FORMAT = "^((([pnbrqkPNBRQK1-8]{1,8})/){7}([pnbrqkPNBRQK1-8]{1,8}))\\s([wb])\\s(-|[KQkq]{1,4})((\\s-)|(\\s[a-h][36])){1,2}?(\\s(\\d+)\\s(\\d+))?$";

    public static StatusPair<FromFEN> validateFEN(final String fen) {
        if (!Pattern.matches(FEN_FORMAT, fen)) {
            return StatusPair.ofFalse();
        }

        return validateForsythEdwardsNotation(fen);
    }

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

    private static StatusPair<FromFEN> validateForsythEdwardsNotation(String fen) {
        final String[] split = fen.split(" ", 2);
        final String board = split[0];
        final String tail = split[1];

        final Color activeColor = fen.contains("w") ? Color.WHITE : Color.BLACK;
        final Optional<Coordinate> passagePawnCaptureCoord = getPassagePawnCaptureCoord(tail);

        return null;
    }

    private static Optional<Coordinate> getPassagePawnCaptureCoord(String tail) {
        final Matcher matcher = passageCoordinatePattern.matcher(tail);
        return matcher.find() ? Optional.of(Coordinate.valueOf(matcher.group())) : Optional.empty();
    }

    /**private static StatusPair<FromFEN> validate(String fen) {
        final String[] split = fen.split(" ", 2);
        final String board = split[0];
        final String tail = split[1];

        final Color moveTurn = fen.contains("w") ? Color.WHITE : Color.BLACK;

        final StatusPair<Byte> fiftyMovesRule = validateFiftyMovesRule(tail);
        if (!fiftyMovesRule.status()) {
            return StatusPair.ofFalse();
        }

        final Matcher matcher = passageCoordinatePattern.matcher(tail);
        final Optional<Coordinate> passage = matcher.find() ? Optional.of(Coordinate.valueOf(matcher.group())) : Optional.empty();

        final boolean invalidRowForCaptureOnPassage = passage.isPresent() && (passage.get().getRow() != 3 && passage.get().getRow() != 6);
        if (invalidRowForCaptureOnPassage) {
            return StatusPair.ofFalse();
        }

        boolean isValidPassage = passage.isEmpty();

        boolean isWhiteKingExists = false;
        Coordinate whiteKingCoordinate = null;

        boolean isBlackKingExists = false;
        Coordinate blackKingCoordinate = null;

        boolean shortWhiteCastling = tail.contains("K");
        boolean isRookForShortWhiteCastlingExists = false;

        boolean longWhiteCastling = tail.contains("Q");
        boolean isRookForLongWhiteCastlingExists = false;

        boolean shortBlackCastling = tail.contains("k");
        boolean isRookForShortBlackCastlingExists = false;

        boolean longBlackCastling = tail.contains("q");
        boolean isRookForLongBlackCastlingExists = false;

        byte countOfWhitePawns = 0;
        byte countOfBlackPawns = 0;

        byte countOfWhiteQueens = 0;
        byte countOfBlackQueens = 0;

        byte countOfWhiteRooks = 0;
        byte countOfBlackRooks = 0;

        byte countOfWhiteKnights = 0;
        byte countOfBlackKnights = 0;

        byte countOfWhiteBishops = 0;
        byte countOfBlackBishops = 0;

        byte materialAdvantageOfWhite = 0;
        byte materialAdvantageOfBlack = 0;

        int row = 8;
        String[] boardRows = board.split("/");
        for (String s : boardRows) {
            int columnNum = 1;
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    columnNum += Character.getNumericValue(c);
                    if (columnNum > 8) {
                        return StatusPair.ofFalse();
                    }
                    continue;
                }
                if (Character.isLetter(c)) {
                    columnNum++;
                    if (columnNum > 8) {
                        return StatusPair.ofFalse();
                    }
                }

                final Coordinate coordinate = Coordinate.of(row, columnNum).orElseThrow();
                switch (c) {
                    case 'K' -> {
                        if (isWhiteKingExists) {
                            return StatusPair.ofFalse();
                        }
                        final boolean isInvalidKingShortCastleCoordinate = shortWhiteCastling && !coordinate.equals(Coordinate.e1);
                        if (isInvalidKingShortCastleCoordinate) {
                            return StatusPair.ofFalse();
                        }
                        final boolean isInvalidKingLongCastleCoordinate = longWhiteCastling && !coordinate.equals(Coordinate.e1);
                        if (isInvalidKingLongCastleCoordinate) {
                            return StatusPair.ofFalse();
                        }

                        isWhiteKingExists = true;
                        whiteKingCoordinate = coordinate;
                    }
                    case 'k' -> {
                        if (isBlackKingExists) {
                            return StatusPair.ofFalse();
                        }
                        final boolean isInvalidKingShortCastleCoordinate = shortBlackCastling && !coordinate.equals(Coordinate.e8);
                        if (isInvalidKingShortCastleCoordinate) {
                            return StatusPair.ofFalse();
                        }
                        final boolean isInvalidKingLongCastleCoordinate = longBlackCastling && !coordinate.equals(Coordinate.e8);
                        if (isInvalidKingLongCastleCoordinate) {
                            return StatusPair.ofFalse();
                        }

                        isBlackKingExists = true;
                        blackKingCoordinate = coordinate;
                    }
                    case 'R' -> {
                        if (++countOfWhiteRooks > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfWhite += AlgebraicNotation.pieceRank(PieceTYPE.R);

                        if (shortWhiteCastling && coordinate.equals(Coordinate.h1)) {
                            isRookForShortWhiteCastlingExists = true;
                        }
                        if (longWhiteCastling && coordinate.equals(Coordinate.a1)) {
                            isRookForLongWhiteCastlingExists = true;
                        }
                    }
                    case 'r' -> {
                        if (++countOfBlackRooks > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfBlack += AlgebraicNotation.pieceRank(PieceTYPE.R);

                        if (shortBlackCastling && coordinate.equals(Coordinate.h8)) {
                            isRookForShortBlackCastlingExists = true;
                        }
                        if (longBlackCastling && coordinate.equals(Coordinate.a8)) {
                            isRookForLongBlackCastlingExists = true;
                        }
                    }
                    case 'Q' -> {
                        if (++countOfWhiteQueens > 9) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfWhite += AlgebraicNotation.pieceRank(PieceTYPE.Q);
                    }
                    case 'q' -> {
                        if (++countOfBlackQueens > 9) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfBlack += AlgebraicNotation.pieceRank(PieceTYPE.Q);
                    }
                    case 'B' -> {
                        if (++countOfWhiteBishops > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfWhite += AlgebraicNotation.pieceRank(PieceTYPE.B);
                    }
                    case 'b' -> {
                        if (++countOfBlackBishops > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfBlack += AlgebraicNotation.pieceRank(PieceTYPE.B);
                    }
                    case 'N' -> {
                        if (++countOfWhiteKnights > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfWhite += AlgebraicNotation.pieceRank(PieceTYPE.N);
                    }
                    case 'n' -> {
                        if (++countOfBlackKnights > 10) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfBlack += AlgebraicNotation.pieceRank(PieceTYPE.N);
                    }
                    case 'P' -> {
                        if (row == 1 || ++countOfWhitePawns > 8) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfWhite += AlgebraicNotation.pieceRank(PieceTYPE.P);

                        if (passage.isPresent() && coordinate.equals(passage.orElseThrow()) && moveTurn.equals(Color.BLACK)) {
                            isValidPassage = true;
                        }
                    }
                    case 'p' -> {
                        if (row == 8 || ++countOfBlackPawns > 8) {
                            return StatusPair.ofFalse();
                        }

                        materialAdvantageOfBlack += AlgebraicNotation.pieceRank(PieceTYPE.P);

                        if (passage.isPresent() && coordinate.equals(passage.orElseThrow()) && moveTurn.equals(Color.WHITE)) {
                            isValidPassage = true;
                        }
                    }
                    default -> throw new IllegalArgumentException("This symbol can`t exists in FEN.");
                }

            }

            row--;
            if (row < 0) {
                return StatusPair.ofFalse();
            }
        }

        if (shortWhiteCastling && !isRookForShortWhiteCastlingExists) {
            return StatusPair.ofFalse();
        }
        if (longWhiteCastling && !isRookForLongWhiteCastlingExists) {
            return StatusPair.ofFalse();
        }
        if (shortBlackCastling && !isRookForShortBlackCastlingExists) {
            return StatusPair.ofFalse();
        }
        if (longBlackCastling && !isRookForLongBlackCastlingExists) {
            return StatusPair.ofFalse();
        }
        if (!isValidPassage) {
            return StatusPair.ofFalse();
        }

        if (!isWhiteKingExists || !isBlackKingExists) {
            return StatusPair.ofFalse();
        }

        final Optional<Pair<Coordinate, Coordinate>> isLastMovementWasPassage = passage.isEmpty() ?
                Optional.empty() :
                Optional.of(passageCoordinates(passage.orElseThrow()));

        Matcher match = patternOf50MovesRuleOnFEN.matcher(tail);
        byte countOfFullMoves = 1;
        if (match.find()) {
            countOfFullMoves = Byte.parseByte(match.group(2).strip());
        }

        final FromFEN result = new FromFEN(
                fen,
                moveTurn,
                whiteKingCoordinate,
                blackKingCoordinate,
                materialAdvantageOfWhite,
                materialAdvantageOfBlack,
                shortWhiteCastling,
                shortBlackCastling,
                longWhiteCastling,
                longBlackCastling,
                isLastMovementWasPassage
        );

        return StatusPair.ofTrue(result);
    }

    private static Pair<Coordinate, Coordinate> passageCoordinates(Coordinate passageCaptureCoordinate) {
        final int row = passageCaptureCoordinate.getRow();
        final int column = passageCaptureCoordinate.columnToInt();

        int startRow = row;
        int endRow = row;
        if (row == 3) {
            startRow--;
            endRow++;
        } else {
            startRow++;
            endRow--;
        }

        return Pair.of(Coordinate.of(startRow, column).orElseThrow(), Coordinate.of(endRow, column).orElseThrow());
    }

    public static StatusPair<Byte> validateFiftyMovesRule(String tail) {
        final Matcher matcher = patternOf50MovesRuleOnFEN.matcher(tail);

        if (matcher.find()) {
            final byte moveCount = Byte.parseByte(matcher.group());
            if (moveCount >= 0 && moveCount < 100) {
                return StatusPair.ofTrue(moveCount);
            }
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue((byte) 0);
    }*/
}
