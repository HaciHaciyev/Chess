package core.project.chess.domain.chess.enumerations;

import java.util.List;
import java.util.stream.Stream;

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

    Direction(int rowDelta, int colDelta) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
    }

    public static List<Direction> allDirections() {
        return List.of(Direction.values());
    }

    public static List<Direction> diagonalDirections() {
        return List.of(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT);
    }

    public static List<Direction> horizontalVerticalDirections() {
        return List.of(LEFT, TOP, RIGHT, BOTTOM);
    }

    public static Direction ofPath(Coordinate begin, Coordinate end) {
        int rowDiff = Math.abs(end.row() - begin.row());
        int colDiff = Math.abs(end.column() - begin.column());

        int absDiff = Math.abs(rowDiff - colDiff);


        if (absDiff != 0 && absDiff != rowDiff && absDiff != colDiff) {
            throw new IllegalArgumentException("Invalid path");
        }

        int rowOffset = Integer.compare(end.row(), begin.row());
        int colOffset = Integer.compare(end.column(), begin.column());

        return Stream.of(values())
                .filter(direction -> direction.rowDelta == rowOffset && direction.colDelta == colOffset)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching direction"));
    }

    public Coordinate apply(Coordinate coordinate) {
        return Coordinate.of(coordinate.row() + rowDelta, coordinate.column() + colDelta);
    }
}
