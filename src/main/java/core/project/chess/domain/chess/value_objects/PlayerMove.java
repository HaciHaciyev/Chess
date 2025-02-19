package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import jakarta.annotation.Nullable;

import java.util.Objects;

public record PlayerMove(Coordinate from, Coordinate to, @Nullable Piece promotion) {

    public PlayerMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    @Override
    public final boolean equals(Object other) {
        var move = (PlayerMove) other;
        return move.from.equals(this.from) && move.to.equals(this.to);
    }
}
