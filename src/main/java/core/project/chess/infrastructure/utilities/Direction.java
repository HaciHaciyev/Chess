package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
// TODO to validate correctness of directions
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

    public StatusPair<Coordinate> apply(Coordinate coordinate) {
        return Coordinate.coordinate(coordinate.getRow() + rowDelta, AlgebraicNotation.columnToInt(coordinate.getColumn()) + colDelta);
    }

    public static Direction ofPath(Coordinate begin, Coordinate end) {
        int rowDiff = Integer.compare(end.getRow(), begin.getRow());
        int colDiff = Integer.compare(end.getColumnAsInt(), begin.getColumnAsInt());

        return Stream.of(values())
                .filter(direction -> direction.rowDelta == rowDiff && direction.colDelta == colDiff)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching direction"));
    }

    public static List<ChessBoard.Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot);

        return Stream.of(
                        topLeft,
                        topRight,
                        bottomLeft,
                        bottomRight
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, predicate);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, predicate);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, predicate);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, predicate);

        return Stream.of(
                        topLeft,
                        topRight,
                        bottomLeft,
                        bottomRight
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
                                                                              Coordinate replace, Piece replacement) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);

        return Stream.of(
                        topLeft,
                        topRight,
                        bottomLeft,
                        bottomRight
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
                                                                              Coordinate ignore, Coordinate end) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, ignore, end);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore, end);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, ignore, end);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore, end);

        return Stream.of(
                        topLeft,
                        topRight,
                        bottomLeft,
                        bottomRight
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }


    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
        var top = TOP.occupiedFieldFrom(chessBoard, pivot);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot);

        return Stream.of(
                        top,
                        bottom,
                        left,
                        right
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        var top = TOP.occupiedFieldFrom(chessBoard, pivot, predicate);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, predicate);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot, predicate);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, predicate);

        return Stream.of(
                        top,
                        bottom,
                        left,
                        right
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot,
                                                                                        Coordinate ignore, Coordinate end) {
        var top = TOP.occupiedFieldFrom(chessBoard, pivot, ignore, end);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, ignore, end);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot, ignore, end);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore, end);

        return Stream.of(
                        top,
                        bottom,
                        left,
                        right
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot,
                                                                                        Coordinate replace, Piece replacement) {
        var top = TOP.occupiedFieldFrom(chessBoard, pivot, replace, replacement);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, replace, replacement);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, replace, replacement);

        return Stream.of(
                        top,
                        bottom,
                        left,
                        right
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> fieldsFromAllDirections(ChessBoard chessBoard, Coordinate pivot) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot);
        var top = TOP.occupiedFieldFrom(chessBoard, pivot);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot);

        return Stream.of(
                        topLeft,
                        top,
                        topRight,
                        left,
                        right,
                        bottomLeft,
                        bottom,
                        bottomRight
                )
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    public static List<ChessBoard.Field> fieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot) {
        var topLeft = TOP_LEFT.allFieldsInDirection(chessBoard, pivot);
        var topRight = TOP_RIGHT.allFieldsInDirection(chessBoard, pivot);
        var bottomLeft = BOTTOM_LEFT.allFieldsInDirection(chessBoard, pivot);
        var bottomRight = BOTTOM_RIGHT.allFieldsInDirection(chessBoard, pivot);

        topLeft.addAll(topRight);
        topLeft.addAll(bottomLeft);
        topLeft.addAll(bottomRight);

        return topLeft;
    }

    public static List<ChessBoard.Field> fieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
        var top = TOP.allFieldsInDirection(chessBoard, pivot);
        var left = LEFT.allFieldsInDirection(chessBoard, pivot);
        var right = RIGHT.allFieldsInDirection(chessBoard, pivot);
        var bottom = BOTTOM.allFieldsInDirection(chessBoard, pivot);

        top.addAll(left);
        top.addAll(right);
        top.addAll(bottom);

        return top;
    }

    public static List<ChessBoard.Field> fieldsOfPathExclusive(ChessBoard chessBoard, Coordinate pivot, Coordinate end) {
        Direction direction = Direction.ofPath(pivot, end);

        var possibleCoordinate = direction.apply(pivot);

        List<ChessBoard.Field> fields = new ArrayList<>();

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(end)) {
                break;
            }

            fields.add(chessBoard.field(coordinate));

            possibleCoordinate = direction.apply(coordinate);
        }

        return fields;
    }

    public static List<ChessBoard.Field> fieldsOfPathInclusive(ChessBoard chessBoard, Coordinate pivot, Coordinate end) {
        Direction direction = Direction.ofPath(pivot, end);

        List<ChessBoard.Field> fields = new ArrayList<>();
        fields.add(chessBoard.field(pivot));

        var possibleCoordinate = direction.apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(end)) {
                fields.add(chessBoard.field(coordinate));
                break;
            }

            fields.add(chessBoard.field(coordinate));

            possibleCoordinate = direction.apply(coordinate);
        }

        return fields;
    }

    public List<ChessBoard.Field> allFieldsInDirection(ChessBoard chessBoard, Coordinate pivot) {
        var possibleCoordinate = apply(pivot);
        List<ChessBoard.Field> fields = new ArrayList<>();

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();
            fields.add(chessBoard.field(coordinate));

            possibleCoordinate = apply(coordinate);
        }

        return fields;
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot) {
        var possibleCoordinate = apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        var possibleCoordinate = apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (predicate.test(field)) {
                return Optional.of(field);
            }

            possibleCoordinate = apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                        Coordinate replace, Piece replacement) {
        var possibleCoordinate = apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(replace)) {
                ChessBoard.Field field = new ChessBoard.Field(replace, replacement);
                return Optional.of(field);
            }

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                        Coordinate ignore, Coordinate replace) {
        var possibleCoordinate = apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(ignore)) {
                possibleCoordinate = apply(coordinate);
                continue;
            }

            if (coordinate.equals(replace)) {
                return Optional.empty();
            }

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = apply(coordinate);
        }

        return Optional.empty();
    }
}
