package core.project.chess.domain.aggregates.chess.value_objects;

import java.util.Objects;

public record AlgebraicNotation(PieceTYPE pieceTYPE, Coordinate from, Coordinate to) {

    public AlgebraicNotation {
        Objects.requireNonNull(pieceTYPE);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    @Override
    public String toString() {
        return String.format("%s : %s -> %s", pieceTYPE, from, to);
    }
}
