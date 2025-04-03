package core.project.chess.domain.chess.enumerations;

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

    private static final Direction[] directions = Direction.values();

    private static final Direction[] diagonalDirections = {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT};

    private static final Direction[] horizontalVerticalDirections = {LEFT, TOP, RIGHT, BOTTOM};

    Direction(int rowDelta, int colDelta) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
    }

    public static Direction[] allDirections() {
        return directions;
    }

    public static Direction[] diagonalDirections() {
        return diagonalDirections;
    }

    public static Direction[] horizontalVerticalDirections() {
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

    public Coordinate apply(Coordinate coordinate) {
        return Coordinate.of(coordinate.row() + rowDelta, coordinate.column() + colDelta);
    }
}
