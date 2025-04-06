package core.project.chess.domain.chess.enumerations;

import jakarta.annotation.Nullable;

public enum SimpleDirection {
    VERTICAL,
    HORIZONTAL,
    DIAGONAL;

    public static @Nullable SimpleDirection directionOf(Coordinate pivot, Coordinate to) {
        if (pivot.row() == to.row() && pivot.column() != to.column()) return HORIZONTAL;
        if (pivot.row() != to.row() && pivot.column() == to.column()) return VERTICAL;
        if (Math.abs(pivot.row() - to.row()) == Math.abs(pivot.column() - to.column())) return DIAGONAL;
        return null;
    }
}
