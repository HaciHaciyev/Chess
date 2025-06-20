package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.FromFEN;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.commons.enumerations.Color;
import core.project.chess.domain.commons.tuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static core.project.chess.domain.chess.value_objects.AlgebraicNotation.*;
import static core.project.chess.domain.commons.enumerations.Color.BLACK;
import static core.project.chess.domain.commons.enumerations.Color.WHITE;

public class ChessNotationsValidator {

    private ChessNotationsValidator() {}

    public static final String COORDINATES = "[a-h][1-8]";

    public static final String INFLUENCE_ON_OPPONENT_KING = "[+#.]?";

    public static final String PAWN_START_MOVEMENT_COORDINATES = "[a-h][2-7]";

    public static final Pattern passageCoordinatePattern = Pattern.compile("\\b[a-h][36]\\b");

    public static final String FEN_FORMAT = "^((([pnbrqkPNBRQK1-8]{1,8})/){7}([pnbrqkPNBRQK1-8]{1,8}))\\s([wb])\\s(-|[KQkq]{1,4})((\\s-)|(\\s[a-h][36])){1,2}?(\\s(\\d+)\\s(\\d+))?$";

    /**
     * Validates a given FEN (Forsyth-Edwards Notation) string.
     * <p>
     * This method performs a basic syntax validation of the FEN string to ensure that:
     * 1. The FEN string consists of exactly 6 parts separated by spaces.
     * 2. The board layout is correct, with valid pieces and empty spaces, and the total number of squares in each row is 8.
     * 3. The castling rights field is properly formatted.
     * 4. The en passant square is valid or marked as '-' for no en passant.
     * <p>
     * However, this method does not perform a complete game state validation. The following checks are not covered here:
     * <ul>
     *   <li><b>Checkmate:</b> This method does not check if either player is in checkmate.</li>
     *   <li><b>Check on non-active king:</b> The method does not verify if a non-active king (the player whose turn it isn't) is in check.</li>
     *   <li><b>Stalemate:</b> The method does not check for a stalemate situation.</li>
     *   <li><b>Non-jumpers on first and last rows:</b> This method does not validate if non-jumping pieces (like rooks, bishops, and queens) are placed on their initial ranks or moved correctly.</li>
     * </ul>
     *
     * These additional game state checks, including whether the positions are logically possible, are handled later by the
     * {@link ChessBoard} class. Specifically, the ChessBoard constructor performs these validations after the pieces have been placed
     * on the board.
     *
     * @param fen A string representing a chess position in Forsyth-Edwards Notation (FEN).
     * @return A {@link StatusPair} object, where the first value indicates whether the FEN string is valid, and the second value contains
     *         a message about the validation result. If invalid, the message will describe the error.
     */
    public static StatusPair<FromFEN> validateFEN(final String fen) {
        return validateForsythEdwardsNotation(fen);
    }

    public static List<AlgebraicNotation> listOfAlgebraicNotations(String pgn) {
        return algebraicNotationsOf(pgn);
    }

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

        throw new IllegalArgumentException("Invalid algebraic notation format: " + algebraicNotation);
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

        if (from.column() != to.column()) {
            throw new IllegalArgumentException("From and to are equal: " + algebraicNotation);
        }

        final boolean validPassage = (from.row() == 2 && to.row() == 4) || (from.row() == 7 && to.row() == 5);
        if (validPassage) {
            return;
        }

        final boolean validMoveDistance = Math.abs(from.row() - to.row()) == 1;
        if (validMoveDistance) {
            final boolean fieldForPromotion = to.row() == 1 || to.row() == 8;
            if (fieldForPromotion) {
                throw new IllegalArgumentException("Promotion field, but no promotion is specified: " + algebraicNotation);
            }

            return;
        }

        throw new IllegalArgumentException("Invalid pawn movement: " + algebraicNotation);
    }

    public static void validatePawnCaptureOperation(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
        final int startColumn = from.column();
        final int endColumn = to.column();
        final int startRow = from.row();
        final int endRow = to.row();

        final boolean diagonalCapture = Math.abs(startRow - endRow) == 1 && Math.abs(startColumn - endColumn) == 1;
        if (diagonalCapture) {
            final boolean fieldForPromotion = to.row() == 1 || to.row() == 8;
            if (fieldForPromotion) {
                throw new IllegalArgumentException("Promotion field, but no promotion is specified: " + algebraicNotation);
            }

            return;
        }

        throw new IllegalArgumentException("Invalid pawn capture status: " + algebraicNotation);
    }

    public static void validatePromotion(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
        final int startColumn = from.column();
        final int endColumn = to.column();
        final int startRow = from.row();
        final int endRow = to.row();

        final boolean validRows = (startRow == 7 && endRow == 8) || (startRow == 2 && endRow == 1);
        if (!validRows) {
            throw new IllegalArgumentException("Invalid coordinates for pawn promotion.");
        }

        final boolean validColumns = (startColumn == endColumn) || Math.abs(startColumn - endColumn) == 1;
        if (!validColumns) {
            throw new IllegalArgumentException("Invalid coordinates for pawn promotion: " + algebraicNotation);
        }
    }

    public static void validateSimpleFigureMovementOrCapture(final String algebraicNotation) {
        final Coordinate from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
        final Coordinate to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
        final int startColumn = from.column();
        final int endColumn = to.column();
        final int startRow = from.row();
        final int endRow = to.row();

        final boolean king = algebraicNotation.charAt(0) == 'K';
        if (king) {
            final boolean surroundField = Math.abs(startRow - endRow) <= 1 && Math.abs(startColumn - endColumn) <= 1;

            if (surroundField) {
                return;
            }

            throw new IllegalArgumentException("Invalid king movement: " + algebraicNotation);
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

            throw new IllegalArgumentException("Invalid queen movement coordinates: " + algebraicNotation);
        }

        final boolean rook = algebraicNotation.charAt(0) == 'R';
        if (rook) {
            if (verticalMove) {
                return;
            }

            if (horizontalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid rook movement coordinates: " + algebraicNotation);
        }

        final boolean bishop = algebraicNotation.charAt(0) == 'B';
        if (bishop) {
            if (diagonalMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid bishop movement coordinates: " + algebraicNotation);
        }

        final boolean knight = algebraicNotation.charAt(0) == 'N';
        if (knight) {
            int differenceOfRow = Math.abs(from.row() - to.row());
            int differenceOfColumn = Math.abs(from.column() - to.column());
            final boolean knightMove =
                    (differenceOfRow == 2 && differenceOfColumn == 1) || (differenceOfRow == 1 && differenceOfColumn == 2);

            if (knightMove) {
                return;
            }

            throw new IllegalArgumentException("Invalid knight movement coordinates: " + algebraicNotation);
        }
    }

    private static StatusPair<FromFEN> validateForsythEdwardsNotation(String fen) {
        if (Objects.isNull(fen)) {
            return StatusPair.ofFalse();
        }
        if (!Pattern.matches(FEN_FORMAT, fen)) {
            return StatusPair.ofFalse();
        }

        final String[] split = fen.split(" ", 2);
        final String board = split[0];
        final String tail = split[1];

        final Color activeColor = fen.contains("w") ? WHITE : BLACK;

        final Optional<Coordinate> passagePawnCoordinate = getPassagePawnCoordinate(tail);
        boolean isValidPassage = passagePawnCoordinate.isEmpty();

        Coordinate whiteKingCoordinate = null;
        Coordinate blackKingCoordinate = null;

        boolean shortWhiteCastling = tail.contains("K");
        boolean isRookForShortWhiteCastlingExists = false;
        boolean longWhiteCastling = tail.contains("Q");
        boolean isRookForLongWhiteCastlingExists = false;
        boolean shortBlackCastling = tail.contains("k");
        boolean isRookForShortBlackCastlingExists = false;
        boolean longBlackCastling = tail.contains("q");
        boolean isRookForLongBlackCastlingExists = false;

        int countOfWhitePawns = 0;
        int countOfBlackPawns = 0;
        int countOfWhiteQueens = 0;
        int countOfBlackQueens = 0;
        int countOfWhiteRooks = 0;
        int countOfBlackRooks = 0;
        int countOfWhiteKnights = 0;
        int countOfBlackKnights = 0;
        int countOfWhiteBishopsOnWhiteFields = 0;
        int countOfWhiteBishopsOnBlackFields = 0;
        int countOfBlackBishopsOnWhiteFields = 0;
        int countOfBlackBishopsOnBlackFields = 0;
        byte materialAdvantageOfWhite = 0;
        byte materialAdvantageOfBlack = 0;

        int row = 8;
        int column = 0;
        for (char c : board.toCharArray()) {
            if (c == '/') {
                row--;
                if (row < 1) {
                    return StatusPair.ofFalse();
                }
                if (column != 8) {
                    return StatusPair.ofFalse();
                }
                column = 0;
                continue;
            }

            if (Character.isDigit(c)) {
                column += Character.getNumericValue(c);
                if (column > 8) {
                    return StatusPair.ofFalse();
                }
                continue;
            }

            column++;
            if (column > 8) {
                return StatusPair.ofFalse();
            }

            final Coordinate coordinate = Coordinate.of(row, column);
            final Color fieldColor = getColorOfField(coordinate);
            switch (c) {
                case 'K' -> {
                    if (Objects.nonNull(whiteKingCoordinate)) {
                        return StatusPair.ofFalse();
                    }

                    whiteKingCoordinate = coordinate;

                    if ((shortWhiteCastling || longWhiteCastling) && !whiteKingCoordinate.equals(e1)) {
                        return StatusPair.ofFalse();
                    }

                    if (Objects.nonNull(blackKingCoordinate)) {
                        final boolean isSurround = Math.abs(whiteKingCoordinate.row() - blackKingCoordinate.row()) <= 1
                                && Math.abs(whiteKingCoordinate.column() - blackKingCoordinate.column()) <= 1;

                        if (isSurround) {
                            return StatusPair.ofFalse();
                        }
                    }
                }
                case 'k' -> {
                    if (Objects.nonNull(blackKingCoordinate)) {
                        return StatusPair.ofFalse();
                    }

                    blackKingCoordinate = coordinate;

                    if ((shortBlackCastling || longBlackCastling) && !blackKingCoordinate.equals(e8)) {
                        return StatusPair.ofFalse();
                    }

                    if (Objects.nonNull(whiteKingCoordinate)) {
                        final boolean isSurround = Math.abs(whiteKingCoordinate.row() - blackKingCoordinate.row()) <= 1
                                && Math.abs(whiteKingCoordinate.column() - blackKingCoordinate.column()) <= 1;

                        if (isSurround) {
                            return StatusPair.ofFalse();
                        }
                    }
                }
                case 'Q' -> {
                    countOfWhiteQueens++;
                    if (countOfWhiteQueens > 9) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfWhite += AlgebraicNotation.pieceValue(PieceTYPE.Q);
                }
                case 'q' -> {
                    countOfBlackQueens++;
                    if (countOfBlackQueens > 9) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfBlack += AlgebraicNotation.pieceValue(PieceTYPE.Q);
                }
                case 'B' -> {
                    if (fieldColor.equals(WHITE)) {
                        countOfWhiteBishopsOnWhiteFields++;
                    } else {
                        countOfWhiteBishopsOnBlackFields++;
                    }

                    if (countOfWhiteBishopsOnWhiteFields + countOfWhiteBishopsOnBlackFields > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfBlack += AlgebraicNotation.pieceValue(PieceTYPE.B);
                }
                case 'b' -> {
                    if (fieldColor.equals(WHITE)) {
                        countOfBlackBishopsOnWhiteFields++;
                    } else {
                        countOfBlackBishopsOnBlackFields++;
                    }

                    if (countOfBlackBishopsOnBlackFields + countOfBlackBishopsOnWhiteFields > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfWhite += AlgebraicNotation.pieceValue(PieceTYPE.B);
                }
                case 'N' -> {
                    countOfWhiteKnights++;
                    if (countOfWhiteKnights > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfWhite += AlgebraicNotation.pieceValue(PieceTYPE.N);
                }
                case 'n' -> {
                    countOfBlackKnights++;
                    if (countOfBlackKnights > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfBlack += AlgebraicNotation.pieceValue(PieceTYPE.N);
                }
                case 'R' -> {
                    countOfWhiteRooks++;
                    if (countOfWhiteRooks > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfWhite += AlgebraicNotation.pieceValue(PieceTYPE.R);

                    if (shortWhiteCastling && !isRookForShortWhiteCastlingExists) {
                        isRookForShortWhiteCastlingExists = coordinate.equals(h1);
                    }

                    if (longWhiteCastling && !isRookForLongWhiteCastlingExists) {
                        isRookForLongWhiteCastlingExists = coordinate.equals(a1);
                    }
                }
                case 'r' -> {
                    countOfBlackRooks++;
                    if (countOfBlackRooks > 10) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfBlack += AlgebraicNotation.pieceValue(PieceTYPE.R);

                    if (shortBlackCastling && !isRookForShortBlackCastlingExists) {
                        isRookForShortBlackCastlingExists = coordinate.equals(h8);
                    }

                    if (longBlackCastling && !isRookForLongBlackCastlingExists) {
                        isRookForLongBlackCastlingExists = coordinate.equals(a8);
                    }
                }
                case 'P' -> {
                    if (coordinate.row() == 1 || coordinate.row() == 8) {
                        return StatusPair.ofFalse();
                    }

                    countOfWhitePawns++;
                    if (countOfWhitePawns > 8) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfWhite += AlgebraicNotation.pieceValue(PieceTYPE.P);

                    final boolean isPassagePawnFound = passagePawnCoordinate.isPresent() && passagePawnCoordinate.orElseThrow().equals(coordinate);
                    if (isPassagePawnFound) {
                        isValidPassage = true;
                    }
                }
                case 'p' -> {
                    if (coordinate.row() == 1 || coordinate.row() == 8) {
                        return StatusPair.ofFalse();
                    }

                    countOfBlackPawns++;
                    if (countOfBlackPawns > 8) {
                        return StatusPair.ofFalse();
                    }

                    materialAdvantageOfBlack += AlgebraicNotation.pieceValue(PieceTYPE.P);

                    final boolean isPassagePawnFound = passagePawnCoordinate.isPresent() && passagePawnCoordinate.orElseThrow().equals(coordinate);
                    if (isPassagePawnFound) {
                        isValidPassage = true;
                    }
                }
                default -> throw new IllegalArgumentException("Invalid FEN: " + fen);
            }
        }

        if (!isValidPassage) {
            return StatusPair.ofFalse();
        }

        if (Objects.isNull(whiteKingCoordinate) || Objects.isNull(blackKingCoordinate)) {
            return StatusPair.ofFalse();
        }

        final boolean isInsufficientMatingMaterial = (materialAdvantageOfWhite <= 3 && materialAdvantageOfBlack == 0 ||
                materialAdvantageOfWhite == 0 && materialAdvantageOfBlack <= 3) &&
                countOfBlackPawns == 0 && countOfWhitePawns == 0;

        if (isInsufficientMatingMaterial) {
            return StatusPair.ofFalse();
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

        final int extraPiecesOfWhites = Math.max(0, countOfWhiteQueens - 1) +
                (Math.max(0, countOfWhiteBishopsOnWhiteFields - 1) + Math.max(0, countOfWhiteBishopsOnBlackFields - 1)) +
                Math.max(0, countOfWhiteKnights - 2) +
                Math.max(0, countOfWhiteRooks - 2);

        final boolean isCountOfExtraPiecesForWhiteValid = extraPiecesOfWhites <= 8 - countOfWhitePawns;
        if (!isCountOfExtraPiecesForWhiteValid) {
            return StatusPair.ofFalse();
        }

        final int extraPiecesOfBlacks = Math.max(0, countOfBlackQueens - 1) +
                (Math.max(0, countOfBlackBishopsOnWhiteFields - 1) + Math.max(0, countOfBlackBishopsOnBlackFields - 1)) +
                Math.max(0, countOfBlackKnights - 2) +
                Math.max(0, countOfBlackRooks - 2);

        final boolean isCountOfExtraPiecesForBlackValid = extraPiecesOfBlacks <= 8 - countOfBlackPawns;
        if (!isCountOfExtraPiecesForBlackValid) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue(new FromFEN(
                fen,
                activeColor,
                whiteKingCoordinate,
                blackKingCoordinate,
                materialAdvantageOfWhite,
                materialAdvantageOfBlack,
                shortWhiteCastling,
                shortBlackCastling,
                longWhiteCastling,
                longBlackCastling,
                passagePawnCoordinate.isEmpty() ? Optional.empty() : Optional.of(passageCoordinates(passagePawnCoordinate.orElseThrow()))
        ));
    }

    private static Color getColorOfField(final Coordinate coordinate) {
        final int row = coordinate.row();
        final int column = coordinate.column();

        if (row % 2 != 0) {
            if (column % 2 != 0) {
                return BLACK;
            }
            return WHITE;
        }

        if (column % 2 != 0) {
            return WHITE;
        }
        return BLACK;
    }

    private static Optional<Coordinate> getPassagePawnCoordinate(String tail) {
        final Matcher matcher = passageCoordinatePattern.matcher(tail);
        if (matcher.find()) {
            final Coordinate captureCoord = Coordinate.valueOf(matcher.group());

            if (captureCoord.row() == 3) {
                return Optional.ofNullable(Coordinate.of(4, captureCoord.column()));
            }

            return Optional.ofNullable(Coordinate.of(5, captureCoord.column()));
        }

        return Optional.empty();
    }

    private static Pair<Coordinate, Coordinate> passageCoordinates(Coordinate endCoordinate) {
        final int row = endCoordinate.row();
        final int column = endCoordinate.column();

        int startRow = row;
        if (row == 4) {
            startRow -= 2;
        } else {
            startRow += 2;
        }

        return Pair.of(Coordinate.of(startRow, column), endCoordinate);
    }

    public static List<AlgebraicNotation> algebraicNotationsOf(String pgn) {
        final List<AlgebraicNotation> algebraicNotations = new ArrayList<>();
        if (Objects.isNull(pgn) || pgn.isBlank()) {
            return algebraicNotations;
        }

        String[] fullMoves = pgn.lines()
                .dropWhile(string -> string.startsWith("[") && string.endsWith("]") || string.isEmpty())
                .reduce("", (a, b) -> a +" " + b)
                .substring(3)
                .split("\\d\\.");

        for (String s : fullMoves) {
            String fullMove = s.strip();
            String[] halfMoves = fullMove.split(" ");

            if (halfMoves.length == 1) {
                algebraicNotations.add(AlgebraicNotation.of(halfMoves[0]));
                continue;
            }

            algebraicNotations.add(AlgebraicNotation.of(halfMoves[0]));
            if (halfMoves[1].equals("...")) {
                continue;
            }
            algebraicNotations.add(AlgebraicNotation.of(halfMoves[1]));
        }

        return algebraicNotations;
    }
}