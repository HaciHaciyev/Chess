package core.project.chess.domain.aggregates.chess.value_objects;

import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AlgebraicNotation {
    private final String algebraicNotation;
    private final Operations operation;
    private final Coordinate from;
    private final Coordinate to;

    public static AlgebraicNotation of(
            Piece piece, Operations operation, Coordinate from, Coordinate to, @Nullable Piece inCaseOfPromotion
    ) {
        Objects.requireNonNull(piece);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        final boolean isCastle = isCastling(piece, from, to);
        if (isCastle) {
            return new AlgebraicNotation(castle(to).getAlgebraicNotation(), Operations.EMPTY, from, to);
        }

        if (operation.equals(Operations.PROMOTION)) {
            Objects.requireNonNull(inCaseOfPromotion);

            if (!(piece instanceof Pawn)) {
                throw new IllegalStateException("Only pawn available for promotion.");
            }

            final boolean legalPromotion = !(inCaseOfPromotion instanceof Pawn) && !(inCaseOfPromotion instanceof King);
            if (!legalPromotion) {
                throw new IllegalStateException("Pawns can`t be promoted to king or pawn.");
            }

            String algebraicNotation = String.format("%s-%s=%s", from, to, pieceToType(inCaseOfPromotion));
            return new AlgebraicNotation(algebraicNotation, operation, from, to);
        }

        if (operation.equals(Operations.EMPTY)) {
            if (piece instanceof Pawn) {
                String algebraicNotation = String.format("%s-%s", from, to);
                return new AlgebraicNotation(algebraicNotation, operation, from, to);
            }

            String algebraicNotation = String.format("%s %s-%s", pieceToType(piece), from, to);
            return new AlgebraicNotation(algebraicNotation, operation, from, to);
        }

        String algebraicNotation = String.format("%s %s %s %s", pieceToType(piece), from, operation, to);
        return new AlgebraicNotation(algebraicNotation, operation, from, to);
    }

    private static String pieceToType(Piece piece) {
        return switch (piece) {
            case King k -> "K";
            case Queen q -> "Q";
            case Rook r -> "R";
            case Bishop b -> "B";
            case Knight n -> "N";
            default -> "";
        };
    }

    public static Castle castle(Coordinate to) {
        final boolean isShortCasting = to.equals(Coordinate.G1) || to.equals(Coordinate.G8);
        if (isShortCasting) {
            return Castle.SHORT_CASTLING;
        }

        return Castle.LONG_CASTLING;
    }

    /** This function can only be used to predetermine the user's intention to make castling,
     * However, this is by no means a final validation of this operation. */
    public static boolean isCastling(Piece piece, Coordinate from, Coordinate to) {
        final boolean isKing = (piece instanceof King);
        if (!isKing) {
            return false;
        }

        final boolean isValidKingPosition = from.equals(Coordinate.E1) || from.equals(Coordinate.E8);
        if (!isValidKingPosition) {
            return false;
        }

        final boolean isCastle = to.equals(Coordinate.C1) || to.equals(Coordinate.G1) ||
                                 to.equals(Coordinate.C8) || to.equals(Coordinate.G8);
        if (!isCastle) {
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlgebraicNotation that = (AlgebraicNotation) o;
        return Objects.equals(algebraicNotation, that.algebraicNotation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(algebraicNotation);
    }

    @Override
    public String toString() {
        return algebraicNotation;
    }

    @Getter
    public enum Operations {
        PROMOTION("="),
        CAPTURE("X"),
        CHECK("+"),
        STALEMATE("."),
        CHECKMATE("#"),
        EMPTY("");

        private final String algebraicNotation;

        Operations(String algebraicNotation) {
            this.algebraicNotation = algebraicNotation;
        }
    }

    @Getter
    public enum Castle {
        SHORT_CASTLING("O-O"), LONG_CASTLING("O-O-O");

        private final String algebraicNotation;

        Castle(String algebraicNotation) {
            this.algebraicNotation = algebraicNotation;
        }
    }

    @Getter
    public enum FigureType {
        K("KING"),
        Q("QUEEN"),
        R("ROOK"),
        B("BISHOP"),
        N("KNIGHT");

        private final String value;

        FigureType(String value) {
            this.value = value;
        }
    }
}
