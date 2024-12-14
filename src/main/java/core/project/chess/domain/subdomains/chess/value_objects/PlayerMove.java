package core.project.chess.domain.subdomains.chess.value_objects;

import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;
import core.project.chess.domain.subdomains.chess.pieces.Piece;
import jakarta.annotation.Nullable;

import java.util.Objects;

public record PlayerMove(Coordinate from, Coordinate to, @Nullable Piece promotion) {

    public PlayerMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }
}
