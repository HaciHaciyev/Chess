package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record King(Color color)
        implements Piece {

    @Override
    public StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        return StatusPair.ofFalse();
    }

    public StatusPair<LinkedHashSet<Operations>> canCastle(ChessBoard chessBoard, Field from, Field to) {
        return StatusPair.ofFalse();
    }

    public boolean safeForKing(ChessBoard chessBoard, Coordinate kingPosition, Coordinate from, Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        return fromDiagonals(chessBoard, kingPosition, to)
                && fromHorizontalAndVertical(chessBoard, kingPosition, to)
                && fromKnight(chessBoard, kingPosition, to);
    }

    private boolean fromDiagonals(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var upperLeftCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn() - 1));

        var upperRightCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn() + 1));

        var downLeftCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn() - 1));

        var downRightCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn() + 1));


        return validateCoordinates(chessBoard, upperLeftCoordinates, to)
                && validateCoordinates(chessBoard, upperRightCoordinates, to)
                && validateCoordinates(chessBoard, downLeftCoordinates, to)
                && validateCoordinates(chessBoard, downRightCoordinates, to);
    }

    private boolean fromHorizontalAndVertical(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var leftHorizontalCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow(), coordinate.getColumn() - 1));

        var rightHorizontalCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow(), coordinate.getColumn() + 1));

        var upperVerticalCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn()));

        var downVerticalCoordinates = getCoordinates(kingPosition, coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn()));

        return validateCoordinates(chessBoard, leftHorizontalCoordinates, to)
                && validateCoordinates(chessBoard, rightHorizontalCoordinates, to)
                && validateCoordinates(chessBoard, upperVerticalCoordinates, to)
                && validateCoordinates(chessBoard, downVerticalCoordinates, to);
    }

    private boolean fromKnight(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        int row = kingPosition.getRow();
        char column = kingPosition.getColumn();

        var knightPos1 = Coordinate.coordinate(row + 1, column - 2);
        var knightPos2 = Coordinate.coordinate(row + 2, column - 1);
        var knightPos3 = Coordinate.coordinate(row + 2, column + 1);
        var knightPos4 = Coordinate.coordinate(row + 1, column + 2);
        var knightPos5 = Coordinate.coordinate(row - 1, column + 2);
        var knightPos6 = Coordinate.coordinate(row - 2, column + 1);
        var knightPos7 = Coordinate.coordinate(row - 2, column - 1);
        var knightPos8 = Coordinate.coordinate(row - 1, column - 2);

        var coordinates = List.of(
                knightPos1,
                knightPos2,
                knightPos3,
                knightPos4,
                knightPos5,
                knightPos6,
                knightPos7,
                knightPos8
        );

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .allMatch(field -> field.isEmpty() || field.getCoordinate().equals(to));
    }

    private boolean validateCoordinates(ChessBoard chessBoard, List<Coordinate> coordinates, Coordinate to) {
        if (coordinates.isEmpty()) {
            return true;
        }

        return coordinates.stream()
                .map(chessBoard::field)
                .allMatch(field -> field.isEmpty() || field.getCoordinate().equals(to));
    }

    private List<Coordinate> getCoordinates(Coordinate currentCoordinate,
                                            Function<Coordinate, StatusPair<Coordinate>> fn) {
        List<Coordinate> coordinates = new ArrayList<>();

        while (true) {
            var pair = fn.apply(currentCoordinate);

            if (!pair.status()) {
                break;
            }

            coordinates.add(pair.valueOrElseThrow());
            currentCoordinate = pair.valueOrElseThrow();
        }

        return coordinates;
    }

    private List<Coordinate> getSurroundingCoordinates(Coordinate kingPosition) {
        int row = kingPosition.getRow();
        char column = kingPosition.getColumn();

        var up = Coordinate.coordinate(row + 1, column);
        var down = Coordinate.coordinate(row - 1, column);
        var left = Coordinate.coordinate(row, column - 1);
        var right = Coordinate.coordinate(row, column + 1);
        var downLeft = Coordinate.coordinate(row - 1, column - 1);
        var downRight = Coordinate.coordinate(row - 1, column + 1);
        var upperLeft = Coordinate.coordinate(row + 1, column - 1);
        var upperRight = Coordinate.coordinate(row + 1, column + 1);

        var coordinates = List.of(
                up,
                down,
                left,
                right,
                upperLeft,
                upperRight,
                downLeft,
                downRight
        );

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .toList();
    }

    /**
     * TODO for Nicat
     */
    public boolean stalemate(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }

    /**
     * TODO for Nicat
     */
    public boolean checkmate(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }

    /**
     * TODO for Nicat
     */
    public boolean check(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }
}
