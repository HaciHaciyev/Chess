package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.util.ChessNotationsValidator;
import core.project.chess.infrastructure.utilities.containers.Pair;
import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * The `AlgebraicNotation` class is responsible for generating the algebraic notation representation of a chess move.
 * Algebraic notation is a standard way of recording chess moves, where each square on the chessboard is assigned a unique
 * coordinate, and the moves are described using these coordinates.
 * <p>
 * This class provides a set of static methods that take in various parameters related to a chess move, such as the piece
 * being moved, the set of status performed during the move (e.g., capture, promotion), the starting and ending
 * coordinates of the move, and the piece being promoted to (if applicable), and generates the corresponding algebraic
 * notation representation.
 * <p>
 * The class also includes some helper methods, such as `isCastling()` and `castle()`, which are used to determine
 * whether a move is a castling move and to get the appropriate algebraic notation for it.
 *
 * @author Hadzhyiev Hadzhy
 */
public class AlgebraicNotation {

    private final byte[] algebraicNotation;

    public String algebraicNotation() {
        return new String(algebraicNotation, StandardCharsets.US_ASCII);
    }

    public byte[] bytes() {
        return algebraicNotation.clone();
    }

    @Override
    public String toString() {
        return algebraicNotation();
    }

    private AlgebraicNotation(byte[] algebraicNotation) {
        this.algebraicNotation = algebraicNotation;
    }

    /**
     * Represents a simple pawn movement, where the pawn moves forward without capturing any piece.
     * The last symbol of the notation indicates a status related to the enemy king or the end of the game as a whole, like
     * check ('+'), checkmate ('#'), stalemate ('.'), or just an empty string if not status with opponent king ('').
     * Examples:
     * "e2-e4"
     * "e2-e4+"
     */
    public static final String SIMPLE_PAWN_MOVEMENT_FORMAT = "%s-%s%s";

    /**
     * Represents a simple movement of a chess piece (other than a pawn), where the piece moves without capturing any piece.
     * Examples:
     * "Nf3-g5"
     * "Nf3-g5#"
     */
    public static final String SIMPLE_FIGURE_MOVEMENT_FORMAT = "%s%s-%s%s";

    /**
     * Represents a pawn capture status, where a pawn captures an opponent's piece.
     * Examples:
     * "e2xd3"
     * "e2xd3."
     */
    public static final String PAWN_CAPTURE_OPERATION_FORMAT = "%s%s%s%s";

    /**
     * Represents a capture status by a chess piece (other than a pawn), where the piece captures an opponent's piece.
     * Examples:
     * "Nf3xd4"
     * "Nf3xd4+"
     */
    public static final String FIGURE_CAPTURE_OPERATION_FORMAT = "%s%s%s%s%s";

    /**
     * Represents a castling move, where the king and the rook move together.
     * Examples:
     * "O-O" (short castling) or "O-O-O" (long castling)
     * "O-O+" (short castling) or "O-O-O+" (long castling)
     */
    public static final String CASTLE_PLUS_OPERATION_FORMAT = "%s%s";

    /**
     * Represents a pawn promotion, where a pawn is promoted to a different piece (e.g., queen, rook, bishop, or knight).
     * Examples:
     * "a7-a8=Q"
     * "a7-a8=Q+"
     */
    public static final String PROMOTION_FORMAT = "%s-%s=%s%s";

    /**
     * Represents a pawn promotion that also includes a capture status.
     * Examples:
     * "a7xb8=Q"
     * "a7xb8=Q#"
     */
    public static final String PROMOTION_PLUS_CAPTURE_OPERATION_FORMAT = "%s%s%s=%s%s";

    private static final byte dash = 45;

    private static final byte equals = 61;

    public static AlgebraicNotation of(final String algebraicNotation) {
        Objects.requireNonNull(algebraicNotation);
        if (algebraicNotation.isBlank()) {
            throw new IllegalArgumentException("Algebraic notation can`t be blank.");
        }

        ChessNotationsValidator.validateAlgebraicNotation(algebraicNotation);

        return new AlgebraicNotation(algebraicNotation.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Generates the algebraic notation representation of a chess move. Able to using only in Domain.
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of status performed during the move (e.g., capture, promotion, check, checkmate, stalemate).
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @param inCaseOfPromotion The piece that the pawn is being promoted to (if applicable).
     * @return An `AlgebraicNotation` object representing the algebraic notation of the move.
     * @throws NullPointerException if any of the required parameters are null.
     */
    public static AlgebraicNotation of(
            final PieceTYPE piece, final Set<ChessBoard.Operations> operationsSet,
            final Coordinate from, final Coordinate to, final @Nullable PieceTYPE inCaseOfPromotion
    ) {
        final boolean promotion = operationsSet.contains(ChessBoard.Operations.PROMOTION);
        if (promotion) return promotionRecording(operationsSet, from, to, inCaseOfPromotion);

        final boolean capture = operationsSet.contains(ChessBoard.Operations.CAPTURE);
        if (capture) {
            if (piece.equals(PieceTYPE.P)) return pawnCaptureRecording(operationsSet, from, to);
            return figureCaptureRecording(piece, operationsSet, from, to);
        }

        return simpleMovementRecording(piece, operationsSet,from, to);
    }

    public static AlgebraicNotation castlingOf(final Castle castle, final Set<ChessBoard.Operations> operations) {
        return castlingRecording(operations, castle);
    }

    /**
     * Generates the algebraic notation representation of a castling move.
     *
     * @param operationsSet The set of status performed during the move.
     * @param castle Determines Short or Long Castle.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the castling move.
     */
    private static AlgebraicNotation castlingRecording(Set<ChessBoard.Operations> operationsSet, Castle castle) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);

        if (opponentKingStatus == ChessBoard.Operations.CONTINUE) return new AlgebraicNotation(castle.bytes());

        int length = castle.bytes().length + 1;
        byte[] bytes = new byte[length];
        System.arraycopy(castle.bytes(), 0, bytes, 0, length - 1);
        bytes[bytes.length - 1] = opponentKingStatus.bytes();
        return new AlgebraicNotation(bytes);
    }

    /**
     * Generates the algebraic notation representation of a pawn capture status.
     *
     * @param operationsSet The set of status performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn capture status.
     */
    private static AlgebraicNotation pawnCaptureRecording(Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);

        if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
            byte[] bytes = {
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    ChessBoard.Operations.CAPTURE.bytes(),
                    to.columnNotationBytes(),
                    to.rowNotationBytes()
            };

            return new AlgebraicNotation(bytes);
        }

        byte[] bytes = {
                from.columnNotationBytes(),
                from.rowNotationBytes(),
                ChessBoard.Operations.CAPTURE.bytes(),
                to.columnNotationBytes(),
                to.rowNotationBytes(),
                opponentKingStatus.bytes()
        };

        return new AlgebraicNotation(bytes);
    }

    /**
     * Generates the algebraic notation representation of a capture status by a chess piece (other than a pawn).
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of status performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the figure capture status.
     */
    private static AlgebraicNotation figureCaptureRecording(
            PieceTYPE piece, Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to
    ) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);

        if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
            byte[] bytes = {
                    piece.bytes(),
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    ChessBoard.Operations.CAPTURE.bytes(),
                    to.columnNotationBytes(),
                    to.rowNotationBytes()
            };

            return new AlgebraicNotation(bytes);
        }

        byte[] bytes = {
                piece.bytes(),
                from.columnNotationBytes(),
                from.rowNotationBytes(),
                ChessBoard.Operations.CAPTURE.bytes(),
                to.columnNotationBytes(),
                to.rowNotationBytes(),
                opponentKingStatus.bytes()
        };

        return new AlgebraicNotation(bytes);
    }

    /**
     * Generates the algebraic notation representation of a simple movement of a chess piece, where the piece moves without capturing any piece.
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of status performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the simple movement.
     */
    private static AlgebraicNotation simpleMovementRecording(
            PieceTYPE piece, Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to
    ) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);
        if (piece == PieceTYPE.P) {
            if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
                byte[] bytes = {
                        from.columnNotationBytes(),
                        from.rowNotationBytes(),
                        dash,
                        to.columnNotationBytes(),
                        to.rowNotationBytes()
                };

                return new AlgebraicNotation(bytes);
            }

            byte[] bytes = {
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    dash,
                    to.columnNotationBytes(),
                    to.rowNotationBytes(),
                    opponentKingStatus.bytes()
            };

            return new AlgebraicNotation(bytes);
        }

        if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
            byte[] bytes = {
                    piece.bytes(),
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    dash,
                    to.columnNotationBytes(),
                    to.rowNotationBytes()
            };

            return new AlgebraicNotation(bytes);
        }

        byte[] bytes = {
                piece.bytes(),
                from.columnNotationBytes(),
                from.rowNotationBytes(),
                dash,
                to.columnNotationBytes(),
                to.rowNotationBytes(),
                opponentKingStatus.bytes()
        };

        return new AlgebraicNotation(bytes);
    }

    /**
     * Generates the algebraic notation representation of a pawn promotion, where a pawn is promoted to a different piece (e.g., queen, rook, bishop, or knight).
     *
     * @param operationsSet The set of status performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @param inCaseOfPromotion The piece that the pawn is being promoted to.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn promotion.
     */
    private static AlgebraicNotation promotionRecording(
            Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to, PieceTYPE inCaseOfPromotion
    ) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);

        if (operationsSet.contains(ChessBoard.Operations.CAPTURE)) {
            if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
                byte[] bytes = {
                        from.columnNotationBytes(),
                        from.rowNotationBytes(),
                        ChessBoard.Operations.CAPTURE.bytes(),
                        to.columnNotationBytes(),
                        to.rowNotationBytes(),
                        equals,
                        inCaseOfPromotion.bytes()
                };

                return new AlgebraicNotation(bytes);
            }

            byte[] bytes = {
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    ChessBoard.Operations.CAPTURE.bytes(),
                    to.columnNotationBytes(),
                    to.rowNotationBytes(),
                    equals,
                    inCaseOfPromotion.bytes(),
                    opponentKingStatus.bytes()
            };

            return new AlgebraicNotation(bytes);
        }

        if (opponentKingStatus == ChessBoard.Operations.CONTINUE) {
            byte[] bytes = {
                    from.columnNotationBytes(),
                    from.rowNotationBytes(),
                    dash,
                    to.columnNotationBytes(),
                    to.rowNotationBytes(),
                    equals,
                    inCaseOfPromotion.bytes()
            };

            return new AlgebraicNotation(bytes);
        }

        byte[] bytes = {
                from.columnNotationBytes(),
                from.rowNotationBytes(),
                dash,
                to.columnNotationBytes(),
                to.rowNotationBytes(),
                equals,
                inCaseOfPromotion.bytes(),
                opponentKingStatus.bytes()
        };

        return new AlgebraicNotation(bytes);
    }

    /**
     * Determines the status of the opponent's king based on the given set of chess board status.
     *
     * @param operationsSet a set of chess board status performed during a move
     * @return the status of the opponent's king, which can be one of the following:
     *         - {@link ChessBoard.Operations#STALEMATE} if the opponent's king is in stalemate
     *         - {@link ChessBoard.Operations#CHECKMATE} if the opponent's king is in checkmate
     *         - {@link ChessBoard.Operations#CHECK} if the opponent's king is in check
     *         - {@link ChessBoard.Operations#CONTINUE} if none of the above conditions are met
     * @throws IllegalArgumentException if the set of status contains more than one status involving the opponent's king or stalemate
     */
    public static ChessBoard.Operations opponentKingStatus(final Set<ChessBoard.Operations> operationsSet) {
        if (operationsSet.contains(ChessBoard.Operations.STALEMATE)) return ChessBoard.Operations.STALEMATE;
        if (operationsSet.contains(ChessBoard.Operations.CHECKMATE)) return ChessBoard.Operations.CHECKMATE;
        if (operationsSet.contains(ChessBoard.Operations.CHECK)) return ChessBoard.Operations.CHECK;
        return ChessBoard.Operations.CONTINUE;
    }

    /**
     * Converts a piece to its corresponding algebraic notation type.
     *
     * @param piece The piece to be converted.
     * @return The algebraic notation type of the piece.
     */
    public static PieceTYPE pieceToType(final Piece piece) {
        return switch (piece) {
            case King k -> PieceTYPE.K;
            case Queen q -> PieceTYPE.Q;
            case Rook r -> PieceTYPE.R;
            case Bishop b -> PieceTYPE.B;
            case Knight n -> PieceTYPE.N;
            default -> PieceTYPE.P;
        };
    }

    public static Piece fromSymbol(String symbol) {
        return switch (symbol) {
            case "K" -> King.of(Color.WHITE);
            case "Q" -> Queen.of(Color.WHITE);
            case "R" -> Rook.of(Color.WHITE);
            case "B" -> Bishop.of(Color.WHITE);
            case "N" -> Knight.of(Color.WHITE);
            case "P" -> Pawn.of(Color.WHITE);
            case "k" -> King.of(Color.BLACK);
            case "q" -> Queen.of(Color.BLACK);
            case "r" -> Rook.of(Color.BLACK);
            case "b" -> Bishop.of(Color.BLACK);
            case "n" -> Knight.of(Color.BLACK);
            case "p" -> Pawn.of(Color.BLACK);
            default -> throw new IllegalArgumentException("Unknown piece symbol: " + symbol);
        };
    }

    public static Piece fromSymbol(PieceTYPE symbol, Color color) {
        return switch (symbol) {
            case K -> King.of(color);
            case Q -> Queen.of(color);
            case R -> Rook.of(color);
            case B -> Bishop.of(color);
            case N -> Knight.of(color);
            case P -> Pawn.of(color);
        };
    }

    public static byte pieceRank(PieceTYPE piece) {
        return switch (piece) {
            case PieceTYPE.P -> 1;
            case PieceTYPE.N, PieceTYPE.B -> 3;
            case PieceTYPE.R -> 5;
            case PieceTYPE.Q -> 9;
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    /**
     * Determines the type of castling move (short or long) based on the ending coordinate.
     *
     * @param to The ending coordinate of the castling move.
     * @return The type of castling move (short or long).
     */
    public static Castle castle(final Coordinate to) {
        final boolean isShortCasting = to.equals(Coordinate.g1) || to.equals(Coordinate.g8);
        if (isShortCasting) return Castle.SHORT_CASTLING;
        return Castle.LONG_CASTLING;
    }

    /**
     * Checks if the given algebraic notation represents a castling move.
     * returns enum Castle or null
     */
    public static Castle isCastling(final AlgebraicNotation algebraicNotation) {
        final byte[] input = algebraicNotation.algebraicNotation;

        final byte[] shortBytes = Castle.SHORT_CASTLING.bytes();
        if (input.length <= 4 &&
                input[0] == shortBytes[0] &&
                input[1] == shortBytes[1] &&
                input[2] == shortBytes[2])
            return Castle.SHORT_CASTLING;

        final byte[] longBytes = Castle.LONG_CASTLING.bytes();
        if (input[0] == longBytes[0] &&
                input[1] == longBytes[1]
                && input[2] == longBytes[2] &&
                input[3] == longBytes[3] &&
                input[4] == longBytes[4])
            return Castle.LONG_CASTLING;

        return null;
    }

    /**
     * Determines the type of promotion for a chess move in algebraic notation.
     *
     * @return a PieceType or null.
     * @throws IllegalStateException if the promotion type is unexpected.
     */
    @Nullable
    public PieceTYPE promotionType() {
        if (isPromotion()) {
            final byte promotionType = this.algebraicNotation[6];

            return switch (promotionType) {
                case 81 -> PieceTYPE.Q;
                case 66 -> PieceTYPE.B;
                case 78 -> PieceTYPE.N;
                case 82 -> PieceTYPE.R;
                default -> throw new IllegalStateException("Unexpected value: " + promotionType);
            };
        }

        return null;
    }

    /**
     * Extracts the "from" and "to" coordinates from the algebraic notation of a chess move. Use isCastling(...) function before.
     *
     * @return a {@link Pair} containing the "from" and "to" coordinates of the move.
     * !!!CHECK if the algebraic notation represents a castling move, as the coordinates cannot be extracted in the same way.
     */
    public Pair<Coordinate, Coordinate> coordinates() {
        final Coordinate from;
        final Coordinate to;

        final boolean startsFromFigureType = isUppercaseLetter(algebraicNotation[0]);

        if (startsFromFigureType) {
            from = Coordinate.fromNotationBytes(algebraicNotation[1], algebraicNotation[2]);
            to = Coordinate.fromNotationBytes(algebraicNotation[4], algebraicNotation[5]);
            return Pair.of(from, to);
        }

        from = Coordinate.fromNotationBytes(algebraicNotation[0], algebraicNotation[1]);
        to = Coordinate.fromNotationBytes(algebraicNotation[3], algebraicNotation[4]);
        return Pair.of(from, to);
    }

    private static boolean isUppercaseLetter(byte b) {
        return (b >= 'A' && b <= 'Z');
    }

    public boolean isPromotion() {
        int length = this.algebraicNotation.length;
        return this.algebraicNotation[length - 2] == equals || this.algebraicNotation[length - 3] == equals;
    }

    public boolean isCapture() {
        if (algebraicNotation.length == 3) return false;
        byte aByte = ChessBoard.Operations.CAPTURE.bytes();
        return algebraicNotation[2] == aByte || algebraicNotation[3] == aByte;
    }

    /**
     * Retrieves the pair of coordinates representing a castling move.
     *
     * @param castle The type of castling move (short or long).
     * @return A Pair of Coordinates representing the castling move.
     */
    public Pair<Coordinate, Coordinate> castlingCoordinates(final Castle castle, final Color color) {
        final boolean shortCastling = castle == Castle.SHORT_CASTLING;

        if (shortCastling) {
            if (color == Color.WHITE) return Pair.of(Coordinate.e1, Coordinate.h1);
            else return Pair.of(Coordinate.e8, Coordinate.h8);
        }

        if (color == Color.WHITE) return Pair.of(Coordinate.e1, Coordinate.a1);
        return Pair.of(Coordinate.e1, Coordinate.a8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgebraicNotation that = (AlgebraicNotation) o;
        return Objects.deepEquals(algebraicNotation, that.algebraicNotation);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(algebraicNotation);
    }

    /**
     * Represents the two types of castling moves: short castling and long castling.
     */
    public enum Castle {
        SHORT_CASTLING("O-O", new byte[]{79,45,79}),
        LONG_CASTLING("O-O-O", new byte[]{79,45,79,45,79});

        private final String algebraicNotation;

        private final byte[] notationInBytes;

        Castle(String algebraicNotation, byte[] notationInBytes) {
            this.algebraicNotation = algebraicNotation;
            this.notationInBytes = notationInBytes;
        }

        public String getAlgebraicNotation() {
            return algebraicNotation;
        }

        private byte[] bytes() {
            return notationInBytes;
        }

        public byte[] getBytes() {
            return notationInBytes.clone();
        }
    }

    /**
     * Represents the different types of chess pieces.
     */
    public enum PieceTYPE {
        K("K", new byte[]{75}),
        Q("Q", new byte[]{81}),
        B("B", new byte[]{66}),
        N("N", new byte[]{78}),
        R("R", new byte[]{82}),
        P("", new byte[]{});

        private final String pieceType;

        private final byte[] pieceTypeInBytes;

        PieceTYPE(String pieceType, byte[] pieceTypeInBytes) {
            this.pieceType = pieceType;
            this.pieceTypeInBytes = pieceTypeInBytes;
        }

        public String getPieceType() {
            return pieceType;
        }

        public byte bytes() {
            return pieceTypeInBytes[0];
        }
    }
}
