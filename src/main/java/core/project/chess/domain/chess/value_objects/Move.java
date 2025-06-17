package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.commons.annotations.Nullable;

import java.util.Objects;

public record Move(Coordinate from, Coordinate to, @Nullable Piece promotion) implements Comparable<Move> {

    public Move {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    @Override
    public String toString() {
        if (promotion != null) return from.toString() + to.toString() + "=" + promotion;
        return from.toString() + to.toString();
    }

    @Override
    public int compareTo(Move m) {
        int ourColFrom = from.column();
        int ourColTo = to.column();
        int ourRowFrom = from.row();
        int ourRowTo = to.row();

        int theirColFrom = m.from.column();
        int theirColTo = m.to.column();
        int theirRowFrom = m.from.row();
        int theirRowTo = m.to.row();

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