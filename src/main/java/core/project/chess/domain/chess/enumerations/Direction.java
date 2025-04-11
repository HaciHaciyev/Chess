package core.project.chess.domain.chess.enumerations;

import jakarta.annotation.Nullable;

import java.util.List;

public enum Direction {
    LEFT(0, -1),
    RIGHT(0, 1),
    TOP(1, 0),
    TOP_LEFT(1, -1),
    TOP_RIGHT(1, 1),
    BOTTOM(-1, 0),
    BOTTOM_LEFT(-1, -1),
    BOTTOM_RIGHT(-1, 1);

    private final int rowDelta;
    private final int colDelta;

    private static final List<Direction> directions = List.of(Direction.values());

    private static final List<Direction> diagonalDirections = List.of(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT);

    private static final List<Direction> horizontalVerticalDirections = List.of(LEFT, TOP, RIGHT, BOTTOM);

    Direction(int rowDelta, int colDelta) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
    }

    public int rowDelta() {
        return rowDelta;
    }

    public int colDelta() {
        return colDelta;
    }

    public static List<Direction> allDirections() {
        return directions;
    }

    public static List<Direction> diagonalDirections() {
        return diagonalDirections;
    }

    public static List<Direction> horizontalVerticalDirections() {
        return horizontalVerticalDirections;
    }

    public static Direction ofPath(Coordinate begin, Coordinate end) {
        int rowOffset = Integer.compare(end.row(), begin.row());
        int colOffset = Integer.compare(end.column(), begin.column());

        for (Direction direction : directions) {
            if (direction.rowDelta == rowOffset && direction.colDelta == colOffset) {
                return direction;
            }
        }

        throw new IllegalArgumentException("No matching direction");
    }

    @Nullable
    public static Direction directionOf(Coordinate pivot, Coordinate to) {
        int rowOffset = Integer.compare(to.row(), pivot.row());
        int colOffset = Integer.compare(to.column(), pivot.column());

        for (Direction direction : directions) {
            if (direction.rowDelta == rowOffset && direction.colDelta == colOffset) return direction;
        }

        return null;
    }

    public Coordinate apply(Coordinate coordinate) {
        return Coordinate.of(coordinate.row() + rowDelta, coordinate.column() + colDelta);
    }

    public boolean isTowardsLowBits() {
        return switch (this) {
            case LEFT, BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> true;
            default -> false;
        };
    }
}
