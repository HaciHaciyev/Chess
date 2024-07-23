package core.project.chess.domain.aggregates.chess.value_objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AlgebraicNotation {
    private final String algebraicNotation;

    /** TODO make of(...) function to resolve operation enumeration*/
    public static AlgebraicNotation of(PieceTYPE pieceTYPE, Coordinate from, Coordinate to) {
        Objects.requireNonNull(pieceTYPE);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        final boolean isCastle = isCastle(pieceTYPE, from, to);
        if (isCastle) {
            return new AlgebraicNotation(
                    castle(to).getAlgebraicNotation()
            );
        }

        String algebraicNotation = String.format("%s %s-%s", pieceTYPE, from, to);
        return new AlgebraicNotation(algebraicNotation);
    }

    public static Castle castle(Coordinate to) {
        final boolean isShortCasting = to.equals(Coordinate.G1) || to.equals(Coordinate.G8) ||
                to.equals(Coordinate.H1) || to.equals(Coordinate.H8);
        if (isShortCasting) {
            return Castle.SHORT_CASTLING;
        }

        return Castle.LONG_CASTLING;
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

    public boolean isCastle() {
        final boolean shortCasting = algebraicNotation.equals(Castle.SHORT_CASTLING.algebraicNotation);
        final boolean longCasting = algebraicNotation.equals(Castle.LONG_CASTLING.algebraicNotation);

        return shortCasting || longCasting;
    }

    public static boolean isCastle(PieceTYPE pieceTYPE, Coordinate from, Coordinate to) {
        final boolean isKing = pieceTYPE.equals(PieceTYPE.K);
        if (!isKing) {
            return false;
        }

        final boolean isValidKingPosition = from.equals(Coordinate.E1) || from.equals(Coordinate.E8);
        if (!isValidKingPosition) {
            return false;
        }

        final boolean isCastle = to.equals(Coordinate.A1) || to.equals(Coordinate.C1) ||
                to.equals(Coordinate.G1) || to.equals(Coordinate.H1) ||
                to.equals(Coordinate.A8) || to.equals(Coordinate.C8) ||
                to.equals(Coordinate.G8) || to.equals(Coordinate.H8);
        if (!isCastle) {
            return false;
        }

        return true;
    }

    @Getter
    public enum Operations {
        CAPTURE("X"), CHECK("+"), CHECKMATE("#");

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
}
