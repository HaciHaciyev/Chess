package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.chess.value_objects.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public enum Direction {

    LEFT(
            coordinate -> Coordinate.coordinate(coordinate.getRow(), AlgebraicNotation.columnToInt(coordinate.getColumn()) - 1)
    ),

    RIGHT(
            coordinate -> Coordinate.coordinate(coordinate.getRow(), AlgebraicNotation.columnToInt(coordinate.getColumn()) + 1)
    ),

    TOP(
            coordinate -> Coordinate.coordinate(coordinate.getRow() + 1, AlgebraicNotation.columnToInt(coordinate.getColumn()))
    ),

    TOP_LEFT(
            coordinate -> Coordinate.coordinate(coordinate.getRow() + 1, AlgebraicNotation.columnToInt(coordinate.getColumn()) - 1)
    ),

    TOP_RIGHT(
            coordinate -> Coordinate.coordinate(coordinate.getRow() + 1, AlgebraicNotation.columnToInt(coordinate.getColumn()) + 1)
    ),

    BOTTOM(
            coordinate -> Coordinate.coordinate(coordinate.getRow() - 1, AlgebraicNotation.columnToInt(coordinate.getColumn()))
    ),

    BOTTOM_LEFT(
            coordinate -> Coordinate.coordinate(coordinate.getRow() - 1, AlgebraicNotation.columnToInt(coordinate.getColumn()) - 1)
    ),

    BOTTOM_RIGHT(
            coordinate -> Coordinate.coordinate(coordinate.getRow() - 1, AlgebraicNotation.columnToInt(coordinate.getColumn()) + 1)
    );


    final Function<Coordinate, StatusPair<Coordinate>> strategy;

    Direction(Function<Coordinate, StatusPair<Coordinate>> strategy) {
        this.strategy = strategy;
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

    public static List<ChessBoard.Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
                                                                              Coordinate ignore) {
        var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, ignore);
        var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore);

        var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, ignore);
        var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore);

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

    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot, Coordinate ignore) {
        var top = TOP.occupiedFieldFrom(chessBoard, pivot, ignore);
        var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, ignore);

        var left = LEFT.occupiedFieldFrom(chessBoard, pivot, ignore);
        var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, ignore);

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

    public static List<ChessBoard.Field> occupiedFieldsFromHorizontalVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
        final Optional<ChessBoard.Field> top = TOP.occupiedFieldFrom(chessBoard, pivot);
        final Optional<ChessBoard.Field> bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot);
        final Optional<ChessBoard.Field> left = LEFT.occupiedFieldFrom(chessBoard, pivot);
        final Optional<ChessBoard.Field> right = RIGHT.occupiedFieldFrom(chessBoard, pivot);

        return Stream.of(top, bottom, left, right)
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
        var topLeft = TOP_LEFT.fieldsFrom(chessBoard, pivot);
        var topRight = TOP_RIGHT.fieldsFrom(chessBoard, pivot);
        var bottomLeft = BOTTOM_LEFT.fieldsFrom(chessBoard, pivot);
        var bottomRight = BOTTOM_RIGHT.fieldsFrom(chessBoard, pivot);

        topLeft.addAll(topRight);
        topLeft.addAll(bottomLeft);
        topLeft.addAll(bottomRight);

        return topLeft;
    }

    public static List<ChessBoard.Field> fieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
        var top = TOP.fieldsFrom(chessBoard, pivot);
        var left = LEFT.fieldsFrom(chessBoard, pivot);
        var right = RIGHT.fieldsFrom(chessBoard, pivot);
        var bottom = BOTTOM.fieldsFrom(chessBoard, pivot);

        top.addAll(left);
        top.addAll(right);
        top.addAll(bottom);

        return top;
    }

    public List<ChessBoard.Field> fieldsUntil(ChessBoard chessBoard, Coordinate pivot, Coordinate end) {
        var possibleCoordinate = strategy.apply(pivot);

        List<ChessBoard.Field> fields = new ArrayList<>();

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(end)) {
                break;
            }

            fields.add(chessBoard.field(coordinate));


            possibleCoordinate = strategy.apply(coordinate);
        }

        return fields;
    }

    public List<ChessBoard.Field> fieldsFrom(ChessBoard chessBoard, Coordinate pivot) {
        var possibleCoordinate = strategy.apply(pivot);
        List<ChessBoard.Field> fields = new ArrayList<>();

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();
            fields.add(chessBoard.field(coordinate));

            possibleCoordinate = strategy.apply(coordinate);
        }

        return fields;
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot) {
        var possibleCoordinate = strategy.apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = strategy.apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot, Coordinate ignore) {
        var possibleCoordinate = strategy.apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(ignore)) {
                continue;
            }

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = strategy.apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                        Coordinate replace, Piece replacement) {
        var possibleCoordinate = strategy.apply(pivot);

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

            possibleCoordinate = strategy.apply(coordinate);
        }

        return Optional.empty();
    }

    public Optional<ChessBoard.Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                        Coordinate ignore, Coordinate replace) {
        var possibleCoordinate = strategy.apply(pivot);

        while (possibleCoordinate.status()) {
            Coordinate coordinate = possibleCoordinate.orElseThrow();

            if (coordinate.equals(ignore)) {
                possibleCoordinate = strategy.apply(coordinate);
                continue;
            }

            if (coordinate.equals(replace)) {
                return Optional.empty();
            }

            ChessBoard.Field field = chessBoard.field(coordinate);
            if (field.isPresent()) {
                return Optional.of(field);
            }

            possibleCoordinate = strategy.apply(coordinate);
        }

        return Optional.empty();
    }
}
