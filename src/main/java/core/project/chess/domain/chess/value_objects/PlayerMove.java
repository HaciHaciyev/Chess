package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import jakarta.annotation.Nullable;

import java.util.Objects;

public record PlayerMove(Coordinate from, Coordinate to, @Nullable Piece promotion) implements Comparable<PlayerMove> {

    public PlayerMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    @Override
    public String toString() {
        return from.toString() + to.toString();
    }

    @Override
    public int compareTo(PlayerMove m) {
        char ourColFrom = from.getColumn();
        char ourColTo = to.getColumn();
        int ourRowFrom = from.getRow();
        int ourRowTo = to.getRow();

        char theirColFrom = m.from.getColumn();
        char theirColTo = m.to.getColumn();
        int theirRowFrom = m.from.getRow();
        int theirRowTo = m.to.getRow();

        if (ourColFrom < theirColFrom) {
            return -1;
        }

        if (ourColFrom > theirColFrom) {
            return 1;
        }

        if (ourRowFrom < theirRowFrom) {
            return -1;
        }

        if (ourRowFrom > theirRowFrom) {
            return  1;
        }

        if (ourColTo < theirColTo) {
            return -1;
        }

        if (ourColTo > theirColTo) {
            return -1;
        }

        if (ourRowTo < theirRowTo) {
            return -1;
        }

        if (ourRowTo > theirRowTo) {
            return 1;
        }

        return 0;
    }
}