package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

        return fromKnights(chessBoard, kingPosition, to)
                && fromPawns(chessBoard, kingPosition, to)
                && fromAnythingElse(chessBoard, kingPosition, to);
    }

    private boolean fromKnights(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var occupiedFields = fieldsOccupiedByKnights(chessBoard, kingPosition);

        if (occupiedFields.isEmpty()) {
            return true;
        }

        return occupiedFields.stream().allMatch(field ->
                field.getCoordinate().equals(to) || field.pieceOptional().orElseThrow().color().equals(color));
    }

    private List<Field> fieldsOccupiedByKnights(ChessBoard chessBoard, Coordinate kingPosition) {
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

        return Stream.of(
                        knightPos1,
                        knightPos2,
                        knightPos3,
                        knightPos4,
                        knightPos5,
                        knightPos6,
                        knightPos7,
                        knightPos8
                )
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private boolean fromPawns(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var bottomLeft = Coordinate.coordinate(kingPosition.getRow() - 1, kingPosition.getColumn() - 1);
        var bottomRight = Coordinate.coordinate(kingPosition.getRow() - 1, kingPosition.getColumn() + 1);

        return safeFromEnemyPawn(chessBoard, bottomLeft, to)
                && safeFromEnemyPawn(chessBoard, bottomRight, to);
    }

    private boolean safeFromEnemyPawn(ChessBoard chessBoard,
                                      StatusPair<Coordinate> possibleCoordinate,
                                      Coordinate to) {
        if (!possibleCoordinate.status()) {
            return true;
        }

        var coordinate = possibleCoordinate.valueOrElseThrow();

        if (coordinate.equals(to)) {
            return true;
        }

        var field = chessBoard.field(possibleCoordinate.valueOrElseThrow());

        if (field.isEmpty()) {
            return true;
        }

        var pieceColor = field.pieceOptional().orElseThrow().color();

        if (pieceColor.equals(color)) {
            return true;
        } else {
            return !(field.pieceOptional().orElseThrow() instanceof Pawn);
        }
    }

    private boolean fromAnythingElse(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var topLeftField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.TOP_LEFT);

        var topField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.TOP);

        var topRightField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.TOP_RIGHT);


        var leftField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.LEFT);
        var rightField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.RIGHT);


        var bottomLeftField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.BOTTOM_LEFT);

        var bottomField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.BOTTOM);

        var bottomRightField = occupiedFieldFromDirection(chessBoard, kingPosition, Direction.BOTTOM_RIGHT);


        var fields = Stream.of(
                        topLeftField,
                        topField,
                        topRightField,
                        leftField,
                        rightField,
                        bottomLeftField,
                        bottomField,
                        bottomRightField
                ).filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .filter(Field::isPresent)
                .toList();


        return validateFields(fields, to);
    }

    private boolean validateFields(List<Field> fields, Coordinate to) {
        if (fields.isEmpty()) {
            return true;
        }

        boolean areEnemiesButCantHurt = fields.stream()
                .map(field -> field.pieceOptional().orElseThrow())
                .noneMatch(piece -> piece instanceof Bishop || piece instanceof Rook || piece instanceof Queen);

        boolean areFriendly = fields.stream()
                .allMatch(field -> field.getCoordinate().equals(to)
                        || field.pieceOptional().orElseThrow().color().equals(color));

        return areEnemiesButCantHurt || areFriendly;
    }

    private Optional<Field> occupiedFieldFromDirection(ChessBoard chessBoard,
                                                       Coordinate currentCoordinate,
                                                       Direction direction) {

        var fn = direction.strategy;

        while (true) {
            var possibleCoordinate = fn.apply(currentCoordinate);

            if (!possibleCoordinate.status()) {
                return Optional.empty();
            }

            Coordinate coordinate = possibleCoordinate.valueOrElseThrow();
            Field field = chessBoard.field(coordinate);

            if (field.isPresent()) {
                return Optional.of(field);
            }
            currentCoordinate = coordinate;
        }
    }

    private List<Coordinate> surroundingCoordinates(Coordinate kingPosition) {
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

        return Stream.of(
                        up,
                        down,
                        left,
                        right,
                        upperLeft,
                        upperRight,
                        downLeft,
                        downRight
                )
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

    private enum Direction {
        TOP_LEFT(coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn() - 1)),

        TOP(coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn())),

        TOP_RIGHT(coordinate ->
                Coordinate.coordinate(coordinate.getRow() + 1, coordinate.getColumn() + 1)),

        LEFT(coordinate ->
                Coordinate.coordinate(coordinate.getRow(), coordinate.getColumn() - 1)),

        RIGHT(coordinate ->
                Coordinate.coordinate(coordinate.getRow(), coordinate.getColumn() + 1)),

        BOTTOM_LEFT(coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn() - 1)),

        BOTTOM(coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn())),

        BOTTOM_RIGHT(coordinate ->
                Coordinate.coordinate(coordinate.getRow() - 1, coordinate.getColumn() + 1));


        final Function<Coordinate, StatusPair<Coordinate>> strategy;

        Direction(Function<Coordinate, StatusPair<Coordinate>> strategy) {
            this.strategy = strategy;
        }
    }
}
