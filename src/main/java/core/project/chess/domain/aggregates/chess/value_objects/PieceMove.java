package core.project.chess.domain.aggregates.chess.value_objects;

import java.util.Objects;

public record PieceMove(Coordinate from, Coordinate to) {

    public PieceMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }
    
}
