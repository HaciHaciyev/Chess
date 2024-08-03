package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.*;
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

    /**
     * Returns true if king is safe from knights
     */
    private boolean fromKnights(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var coordinates = knightsThreateningCoordinates(kingPosition);

        // get occupied fields from coordinates
        var knights = coordinates.stream()
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();

        if (knights.isEmpty()) {
            return true;
        }

        boolean isNotKnight = knights.stream().noneMatch(field -> field.pieceOptional().orElseThrow() instanceof Knight);

        boolean isEaten = knights.stream().allMatch(field -> field.getCoordinate().equals(to));

        boolean isFriendly = knights.stream().allMatch(field -> field.pieceOptional().orElseThrow().color().equals(color));

        return isNotKnight || isEaten || isFriendly;
    }

    /**
     * returns an immutable list of coordinates from which knight can threaten pivot
     */
    private List<Coordinate> knightsThreateningCoordinates(Coordinate pivot) {
        int row = pivot.getRow();
        char col = pivot.getColumn();

        var knightPos1 = Coordinate.coordinate(row + 1, col - 2);
        var knightPos2 = Coordinate.coordinate(row + 2, col - 1);
        var knightPos3 = Coordinate.coordinate(row + 2, col + 1);
        var knightPos4 = Coordinate.coordinate(row + 1, col + 2);
        var knightPos5 = Coordinate.coordinate(row - 1, col + 2);
        var knightPos6 = Coordinate.coordinate(row - 2, col + 1);
        var knightPos7 = Coordinate.coordinate(row - 2, col - 1);
        var knightPos8 = Coordinate.coordinate(row - 1, col - 2);

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
                .toList();
    }

    /**
     * Returns true if king is safe from pawns
     */
    private boolean fromPawns(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var coordinates = pawnsThreateningCoordinate(kingPosition);

        var pawns = coordinates.stream()
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();

        if (pawns.isEmpty()) {
            return true;
        }

        boolean isNotPawn = pawns.stream().noneMatch(pawn -> pawn.pieceOptional().orElseThrow() instanceof Pawn);

        boolean isEaten = pawns.stream().allMatch(pawn -> pawn.getCoordinate().equals(to));

        boolean isFriendly = pawns.stream().allMatch(pawn -> pawn.pieceOptional().orElseThrow().color().equals(color));

        return isNotPawn || isEaten || isFriendly;
    }

    /**
     * returns an immutable list of coordinates from which pawn can threaten pivot
     */
    private List<Coordinate> pawnsThreateningCoordinate(Coordinate pivot) {
        return Stream.of(
                        Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() - 1),
                        Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() + 1)
                ).filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .toList();
    }

    /**
     * Returns true if king is safe from bishops, rooks and queens
     */
    private boolean fromAnythingElse(ChessBoard chessBoard, Coordinate kingPosition, Coordinate imaginary) {
        var coordinates = othersThreateningCoordinate(kingPosition, imaginary);

        var occupiedFields = coordinates.stream()
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();

        if (occupiedFields.isEmpty()) {
            return true;
        }

        boolean areEnemiesButCantHurt = occupiedFields.stream()
                .map(field -> field.pieceOptional().orElseThrow())
                .noneMatch(piece -> piece instanceof Bishop
                        || piece instanceof Rook
                        || piece instanceof Queen);

        boolean areFriendly = occupiedFields.stream()
                .allMatch(field -> field.getCoordinate().equals(imaginary)
                        || field.pieceOptional().orElseThrow().color().equals(color));

        return areEnemiesButCantHurt || areFriendly;
    }

    /**
     * returns an immutable list of coordinates from which bishop, rook or queen can threaten pivot
     */
    private List<Coordinate> othersThreateningCoordinate(Coordinate pivot, Coordinate imaginary) {

        var topLeft = Direction.TOP_LEFT.coordinatesFrom(pivot, imaginary);
        var top = Direction.TOP.coordinatesFrom(pivot, imaginary);
        var topRight = Direction.TOP_RIGHT.coordinatesFrom(pivot, imaginary);


        var left = Direction.LEFT.coordinatesFrom(pivot, imaginary);
        var right = Direction.RIGHT.coordinatesFrom(pivot, imaginary);


        var bottomLeft = Direction.BOTTOM_LEFT.coordinatesFrom(pivot, imaginary);
        var bottom = Direction.BOTTOM.coordinatesFrom(pivot, imaginary);
        var bottomRight = Direction.BOTTOM_RIGHT.coordinatesFrom(pivot, imaginary);


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
                .flatMap(Collection::stream)
                .toList();
    }

    // TODO WIP
    private List<Field> fieldsEndangeredFromDirection(ChessBoard chessBoard,
                                                      Coordinate currentCoordinate,
                                                      Direction direction) {
        var fn = direction.strategy;
        var fields = new ArrayList<Field>();

        while (true) {
            var possibleCoordinate = fn.apply(currentCoordinate);

            if (!possibleCoordinate.status()) {
                break;
            }

            Coordinate coordinate = possibleCoordinate.valueOrElseThrow();
            Field field = chessBoard.field(coordinate);
            fields.add(field);

            if (field.isPresent()) {
                break;
            }

            currentCoordinate = coordinate;
        }

        return fields;
    }

    // TODO NO USE YET
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

    // TODO
    public boolean check(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();

        return switch (piece) {
            case Pawn _ -> pawnCheck(chessBoard, to);
            case King _ -> knightCheck(chessBoard, to);
            case Bishop _ -> bishopCheck(chessBoard, to);
            default -> throw new IllegalStateException("What da hell are u doing bruv: " + piece);
        };
    }

    // TODO
    private boolean bishopCheck(ChessBoard chessBoard, Coordinate to) {
        var fields = fieldsEndangeredByBishop(chessBoard, to);

        return fields.stream().anyMatch(this::enemyKingIsUnderCheck);
    }

    // TODO
    private List<Field> fieldsEndangeredByBishop(ChessBoard chessBoard, Coordinate pivot) {
        return null;
    }

    // TODO
    private boolean knightCheck(ChessBoard chessBoard, Coordinate to) {
        return false;
    }

    // TODO
    private boolean pawnCheck(ChessBoard chessBoard, Coordinate to) {
        return false;
    }

    // TODO
    private List<Field> fieldsEndangeredByPawn(ChessBoard chessBoard, Coordinate pivot) {
        return null;
    }

    // TODO
    private boolean enemyKingIsUnderCheck(Field field) {
        if (field.isEmpty()) {
            return false;
        }

        Piece piece = field.pieceOptional().orElseThrow();

        if (piece.color().equals(color)) {
            return false;
        }

        return piece instanceof King;
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

        /**
         * Returns an immutable list of coordinates from pivot going towards specified direction.
         * Iteration ends as soon as it reaches boundaries of the board or reaches the coordinate of imaginary piece
         */
        public List<Coordinate> coordinatesFrom(Coordinate pivot, Coordinate imaginary) {
            var coordinates = new ArrayList<Coordinate>();

            while (true) {
                var possibleCoordinate = strategy.apply(pivot);

                if (!possibleCoordinate.status()) {
                    break;
                }

                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();
                coordinates.add(coordinate);

                if (coordinate.equals(imaginary)) {
                    break;
                }

                pivot = coordinate;
            }

            return coordinates;
        }
    }
}
