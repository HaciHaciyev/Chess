package core.project.chess.domain.aggregates.chess.value_objects;

import java.util.Objects;
import java.util.Optional;

public record Field(Coordinate coordinate, Optional<Piece> piece) {

    public Field {
        Objects.requireNonNull(coordinate);
        Objects.requireNonNull(piece);
    }
}
