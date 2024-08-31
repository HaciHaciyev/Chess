package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Bishop;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.pieces.*;
import core.project.chess.infrastructure.utilities.OptionalArgument;
import core.project.chess.infrastructure.utilities.Pair;
import core.project.chess.infrastructure.utilities.StatusPair;
import core.project.chess.infrastructure.utilities.ChessNotationValidator;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

/**
 * The `AlgebraicNotation` class is responsible for generating the algebraic notation representation of a chess move.
 * Algebraic notation is a standard way of recording chess moves, where each square on the chessboard is assigned a unique
 * coordinate, and the moves are described using these coordinates.
 * <p>
 * This class provides a set of static methods that take in various parameters related to a chess move, such as the piece
 * being moved, the set of operations performed during the move (e.g., capture, promotion), the starting and ending
 * coordinates of the move, and the piece being promoted to (if applicable), and generates the corresponding algebraic
 * notation representation.
 * <p>
 * The class also includes some helper methods, such as `isCastling()` and `castle()`, which are used to determine
 * whether a move is a castling move and to get the appropriate algebraic notation for it.
 */
public class AlgebraicNotation {

    private final String algebraicNotation;

    public String algebraicNotation() {
        return algebraicNotation;
    }

    private AlgebraicNotation(String algebraicNotation) {
        this.algebraicNotation = algebraicNotation;
    }

    /**
     * Represents a simple pawn movement, where the pawn moves forward without capturing any piece.
     * The last symbol of the notation indicates an operation related to the enemy king or the end of the game as a whole, like
     * check ('+'), checkmate ('#'), stalemate ('.'), or just an empty string if not operation with opponent king ('').
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
     * Represents a pawn capture operation, where a pawn captures an opponent's piece.
     * Examples:
     * "e2xd3"
     * "e2xd3."
     */
    public static final String PAWN_CAPTURE_OPERATION_FORMAT = "%s%s%s%s";

    /**
     * Represents a capture operation by a chess piece (other than a pawn), where the piece captures an opponent's piece.
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
     * Represents a pawn promotion that also includes a capture operation.
     * Examples:
     * "a7xb8=Q"
     * "a7xb8=Q#"
     */
    public static final String PROMOTION_PLUS_CAPTURE_OPERATION_FORMAT = "%s%s%s=%s%s";

    public static AlgebraicNotation fromRepository(final String algebraicNotation) {
        Objects.requireNonNull(algebraicNotation);
        if (algebraicNotation.isBlank()) {
            throw new IllegalArgumentException("Algebraic notation can`t be black.");
        }

        ChessNotationValidator.validate(algebraicNotation);

        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Generates the algebraic notation representation of a chess move. Able to using only in Domain.
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of operations performed during the move (e.g., capture, promotion, check, checkmate, stalemate).
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @param inCaseOfPromotion The piece that the pawn is being promoted to (if applicable).
     * @return An `AlgebraicNotation` object representing the algebraic notation of the move.
     * @throws NullPointerException if any of the required parameters are null.
     */
    static AlgebraicNotation of(
            final PieceTYPE piece, final Set<ChessBoard.Operations> operationsSet,
            final Coordinate from, final Coordinate to, final @OptionalArgument PieceTYPE inCaseOfPromotion
    ) {

        final boolean castle = isCastling(piece, from, to);
        if (castle) {
            if (operationsSet.contains(ChessBoard.Operations.CAPTURE) || operationsSet.contains(ChessBoard.Operations.PROMOTION)) {
                throw new IllegalArgumentException("Invalid set of operations.");
            }

            return castlingRecording(operationsSet, to);
        }

        final boolean promotion = operationsSet.contains(ChessBoard.Operations.PROMOTION);
        if (promotion) {
            if (!piece.equals(PieceTYPE.P)) {
                throw new IllegalArgumentException("Only pawns available for promotion.");
            }

            return promotionRecording(operationsSet, from, to, inCaseOfPromotion);
        }

        final boolean capture = operationsSet.contains(ChessBoard.Operations.CAPTURE);
        if (capture) {
            if (piece.equals(PieceTYPE.P)) {
                return pawnCaptureRecording(operationsSet, from, to);
            }

            return figureCaptureRecording(piece, operationsSet, from, to);
        }

        return simpleMovementRecording(piece, operationsSet,from, to);
    }

    /**
     * Generates the algebraic notation representation of a castling move.
     *
     * @param operationsSet The set of operations performed during the move.
     * @param finalCoordinate The ending coordinate of the castling move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the castling move.
     */
    private static AlgebraicNotation castlingRecording(Set<ChessBoard.Operations> operationsSet, Coordinate finalCoordinate) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);
        final String algebraicNotation = CASTLE_PLUS_OPERATION_FORMAT.formatted(
                castle(finalCoordinate).getAlgebraicNotation(), opponentKingStatus.getAlgebraicNotation()
        );

        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Generates the algebraic notation representation of a pawn capture operation.
     *
     * @param operationsSet The set of operations performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn capture operation.
     */
    private static AlgebraicNotation pawnCaptureRecording(Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);
        final String algebraicNotation = PAWN_CAPTURE_OPERATION_FORMAT.formatted(
                from, ChessBoard.Operations.CAPTURE.getAlgebraicNotation(), to, opponentKingStatus.getAlgebraicNotation()
        );

        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Generates the algebraic notation representation of a capture operation by a chess piece (other than a pawn).
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of operations performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the figure capture operation.
     */
    private static AlgebraicNotation figureCaptureRecording(
            PieceTYPE piece, Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to
    ) {
        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);
        final String algebraicNotation = FIGURE_CAPTURE_OPERATION_FORMAT.formatted(
                piece, from, ChessBoard.Operations.CAPTURE.getAlgebraicNotation(), to, opponentKingStatus.getAlgebraicNotation()
        );

        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Generates the algebraic notation representation of a simple movement of a chess piece, where the piece moves without capturing any piece.
     *
     * @param piece The piece being moved.
     * @param operationsSet The set of operations performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the simple movement.
     */
    private static AlgebraicNotation simpleMovementRecording(
            PieceTYPE piece, Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to
    ) {
        if (piece.equals(PieceTYPE.P)) {
            final String algebraicNotation = SIMPLE_PAWN_MOVEMENT_FORMAT.formatted(
                    from, to, opponentKingStatus(operationsSet).getAlgebraicNotation()
            );

            return new AlgebraicNotation(algebraicNotation);
        }

        final String algebraicNotation = SIMPLE_FIGURE_MOVEMENT_FORMAT.formatted(
                piece, from, to, opponentKingStatus(operationsSet).getAlgebraicNotation()
        );

        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Generates the algebraic notation representation of a pawn promotion, where a pawn is promoted to a different piece (e.g., queen, rook, bishop, or knight).
     *
     * @param operationsSet The set of operations performed during the move.
     * @param from The starting coordinate of the move.
     * @param to The ending coordinate of the move.
     * @param inCaseOfPromotion The piece that the pawn is being promoted to.
     * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn promotion.
     */
    private static AlgebraicNotation promotionRecording(
            Set<ChessBoard.Operations> operationsSet, Coordinate from, Coordinate to, PieceTYPE inCaseOfPromotion
    ) {
        final String algebraicNotation;

        final ChessBoard.Operations opponentKingStatus = opponentKingStatus(operationsSet);

        if (operationsSet.contains(ChessBoard.Operations.CAPTURE)) {
            algebraicNotation = PROMOTION_PLUS_CAPTURE_OPERATION_FORMAT.formatted(
                    from, ChessBoard.Operations.CAPTURE.getAlgebraicNotation(), to, inCaseOfPromotion.getPieceType(), opponentKingStatus.getAlgebraicNotation()
            );

            return new AlgebraicNotation(algebraicNotation);
        }

        algebraicNotation = PROMOTION_FORMAT.formatted(from, to, inCaseOfPromotion, opponentKingStatus.getAlgebraicNotation());
        return new AlgebraicNotation(algebraicNotation);
    }

    /**
     * Determines the status of the opponent's king based on the given set of chess board operations.
     *
     * @param operationsSet a set of chess board operations performed during a move
     * @return the status of the opponent's king, which can be one of the following:
     *         - {@link ChessBoard.Operations#STALEMATE} if the opponent's king is in stalemate
     *         - {@link ChessBoard.Operations#CHECKMATE} if the opponent's king is in checkmate
     *         - {@link ChessBoard.Operations#CHECK} if the opponent's king is in check
     *         - {@link ChessBoard.Operations#EMPTY} if none of the above conditions are met
     * @throws IllegalArgumentException if the set of operations contains more than one operation involving the opponent's king or stalemate
     */
    static ChessBoard.Operations opponentKingStatus(final Set<ChessBoard.Operations> operationsSet) {
        int opponentKingStatusCount = 0;
        if (operationsSet.contains(ChessBoard.Operations.STALEMATE)) {
            opponentKingStatusCount++;
        }
        if (operationsSet.contains(ChessBoard.Operations.CHECKMATE)) {
            opponentKingStatusCount++;
        }
        if (operationsSet.contains(ChessBoard.Operations.CHECK)) {
            opponentKingStatusCount++;
        }

        if (opponentKingStatusCount > 1) {
            throw new IllegalArgumentException(
                    "A move must have only one operation involving the enemy King or stalemate, an invalid set of operations."
            );
        }

        if (operationsSet.contains(ChessBoard.Operations.STALEMATE)) {
            return ChessBoard.Operations.STALEMATE;
        }
        if (operationsSet.contains(ChessBoard.Operations.CHECKMATE)) {
            return ChessBoard.Operations.CHECKMATE;
        }
        if (operationsSet.contains(ChessBoard.Operations.CHECK)) {
            return ChessBoard.Operations.CHECK;
        }

        return ChessBoard.Operations.EMPTY;
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
            case Knight k -> PieceTYPE.N;
            default -> PieceTYPE.P;
        };
    }

    public static int columnToInt(char startColumn) {
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

    /**
     * Determines the type of castling move (short or long) based on the ending coordinate.
     *
     * @param to The ending coordinate of the castling move.
     * @return The type of castling move (short or long).
     */
    static Castle castle(final Coordinate to) {
        final boolean isShortCasting = to.equals(Coordinate.G1) || to.equals(Coordinate.G8);
        if (isShortCasting) {
            return Castle.SHORT_CASTLING;
        }

        return Castle.LONG_CASTLING;
    }

    /**
     * Checks if the given algebraic notation represents a castling move.
     *
     * @param algebraicNotation the algebraic notation to be checked
     * @return a {@link StatusPair} containing a boolean value indicating whether the
     *         given algebraic notation represents a castling move, and if so, the
     *         corresponding {@link Castle} instance (either {@link Castle#SHORT_CASTLING}
     *         or {@link Castle#LONG_CASTLING}).
     */
    public static StatusPair<Castle> isCastling(final AlgebraicNotation algebraicNotation) {
        final String algebraicNotationSTR = algebraicNotation.algebraicNotation();

        final boolean shortCasting = algebraicNotationSTR
                .equals(Castle.SHORT_CASTLING.getAlgebraicNotation())
                || algebraicNotationSTR.substring(0, algebraicNotationSTR.length() - 1)
                .equals(Castle.SHORT_CASTLING.getAlgebraicNotation());
        if (shortCasting) {
            return StatusPair.ofTrue(Castle.SHORT_CASTLING);
        }

        final boolean longCasting = algebraicNotationSTR
                .equals(Castle.LONG_CASTLING.getAlgebraicNotation())
                || algebraicNotationSTR.substring(0, algebraicNotationSTR.length() - 1)
                .equals(Castle.LONG_CASTLING.getAlgebraicNotation());
        if (longCasting) {
            return StatusPair.ofTrue(Castle.LONG_CASTLING);
        }

        return StatusPair.ofFalse();
    }

    /**
     * This function can only be used to predetermine the user's intention to make castling,
     * However, this is by no means a final validation of this operation.
     */
    public static boolean isCastling(final PieceTYPE piece, final Coordinate from, final Coordinate to) {
        final boolean isKing = piece.equals(PieceTYPE.K);
        if (!isKing) {
            return false;
        }

        final boolean isValidKingPosition = from.equals(Coordinate.E1) || from.equals(Coordinate.E8);
        if (!isValidKingPosition) {
            return false;
        }

        return to.equals(Coordinate.C1) || to.equals(Coordinate.G1) ||
                to.equals(Coordinate.C8) || to.equals(Coordinate.G8);
    }

    /**
     * Extracts the "from" and "to" coordinates from the algebraic notation of a chess move. Use isCastling(...) function before.
     *
     * @return a {@link Pair} containing the "from" and "to" coordinates of the move.
     * @throws IllegalStateException if the algebraic notation represents a castling move, as the coordinates cannot be extracted in the same way.
     */
    public Pair<Coordinate, Coordinate> coordinates() {
        if (AlgebraicNotation.isCastling(this).status()) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        final Coordinate from;
        final Coordinate to;

        final boolean startFromFigureType = Character.isLetter(algebraicNotation.charAt(0)) && Character.isLetter(algebraicNotation.charAt(1));
        if (startFromFigureType) {
            from = Coordinate.valueOf(algebraicNotation.substring(1, 3));
            to = Coordinate.valueOf(algebraicNotation.substring(4, 6));

            return Pair.of(from, to);
        }

        from = Coordinate.valueOf(algebraicNotation.substring(0, 2));
        to = Coordinate.valueOf(algebraicNotation.substring(3, 5));

        return Pair.of(from, to);
    }

    /**
     * Retrieves the pair of coordinates representing a castling move.
     *
     * @param castle The type of castling move (short or long).
     * @return A Pair of Coordinates representing the castling move.
     */
    public Pair<Coordinate, Coordinate> castlingCoordinates(final Castle castle, final Color color) {
        final boolean shortCastling = castle.equals(Castle.SHORT_CASTLING);

        if (shortCastling) {

            if (color.equals(Color.WHITE)) {
                return Pair.of(Coordinate.E1, Coordinate.H1);
            } else {
                return Pair.of(Coordinate.E8, Coordinate.H8);
            }

        }

        if (color.equals(Color.WHITE)) {
            return Pair.of(Coordinate.E1, Coordinate.A1);
        } else {
            return Pair.of(Coordinate.E1, Coordinate.A8);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlgebraicNotation that)) return false;

        return algebraicNotation.equals(that.algebraicNotation);
    }

    @Override
    public int hashCode() {
        return algebraicNotation.hashCode();
    }

    /**
     * Represents the two types of castling moves: short castling and long castling.
     */
    @Getter
    public enum Castle {
        SHORT_CASTLING("O-O"), LONG_CASTLING("O-O-O");

        private final String algebraicNotation;

        Castle(String algebraicNotation) {
            this.algebraicNotation = algebraicNotation;
        }
    }

    /**
     * Represents the different types of chess pieces.
     */
    @Getter
    public enum PieceTYPE {
        K("K"), Q("Q"), B("B"), N("N"), R("R"), P("");

        private final String pieceType;

        PieceTYPE(String pieceType) {
            this.pieceType = pieceType;
        }
    }
}