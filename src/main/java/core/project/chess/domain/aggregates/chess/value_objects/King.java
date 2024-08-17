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
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) {
            return StatusPair.ofFalse();
        }

        Field startField = chessBoard.field(from);
        Field endField = chessBoard.field(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        if (!(startField.pieceOptional().get() instanceof King (var kingColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(kingColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        final boolean validKingMovement = isValidKingMovementCoordinates(chessBoard, startField, endField);
        if (!validKingMovement) {
            return StatusPair.ofFalse();
        }

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();
        setOfOperations.add(influenceOnTheOpponentKing(chessBoard, from, to));

        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && !endField.pieceOptional().get().color().equals(kingColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    public boolean safeForKing(final ChessBoard chessBoard, final Coordinate kingPosition, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (kingPosition.equals(from)) {
            return validateKingMovementForSafety(chessBoard, from, to);
        }

        return validatePieceMovementForKingSafety(chessBoard, kingPosition, from, to);
    }

    public boolean check(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        Coordinate king = getOurKing(chessBoard);

        return switch (piece) {
            case Pawn _ -> pawnMoved(chessBoard, king, from, to);
            case Knight _ -> knightMoved(chessBoard, king, from, to);
            case Bishop _ -> validateDirections(chessBoard, king, from, to, Bishop.class);
            case Rook _ -> validateDirections(chessBoard, king, from, to, Rook.class);
            case Queen _ -> validateDirections(chessBoard, king, from, to, Queen.class);
            case King _ -> validateDirections(chessBoard, king, from, to, King.class);
        };
    }

    /** In process to implementation.*/
    public boolean checkmate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        boolean check = check(chessBoard, from, to);
        boolean surrounded = surroundingFields(chessBoard, from).stream()
                .allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));

        return check && surrounded;
    }

    /** In process to implementation.*/
    public boolean stalemate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        return surroundingFields(chessBoard, from).stream()
                .allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
    }

    private boolean isValidKingMovementCoordinates(ChessBoard chessBoard, Field startField, Field endField) {
        final Coordinate from = startField.getCoordinate();
        final Coordinate to = endField.getCoordinate();
        final int startColumn = columnToInt(from.getColumn());
        final int endColumn = columnToInt(to.getColumn());
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean surroundField = Math.abs(startColumn - endColumn) <= 1 && Math.abs(startRow - endRow) <= 1;
        if (surroundField) {
            return true;
        }

        return chessBoard.isCastling(startField.pieceOptional().orElseThrow(), from, to);
    }

    private boolean validateKingMovementForSafety(ChessBoard chessBoard, Coordinate previousKing, Coordinate futureKing) {
        var pawns = pawnsThreateningCoordinate(chessBoard, futureKing, color);

        for (Field possiblePawn : pawns) {
            Piece pawn = possiblePawn.pieceOptional().orElseThrow();

            boolean isEnemyPawn = pawn instanceof Pawn && !pawn.color().equals(color);

            if (isEnemyPawn) {
                return false;
            }
        }

        var knights = knightAttackPositions(chessBoard, futureKing);

        for (Field possibleKnight : knights) {
            Piece knight = possibleKnight.pieceOptional().orElseThrow();

            boolean isEnemyKnight = knight instanceof Knight && !knight.color().equals(color);

            if (isEnemyKnight) {
                return false;
            }
        }

        var diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, futureKing, previousKing);

        for (Field field : diagonalFields) {
            Piece piece = field.pieceOptional().orElseThrow();

            boolean isEnemyBishopOrQueenOrKing = (piece instanceof Bishop || piece instanceof Queen || piece instanceof King)
                    && !piece.color().equals(color);

            if (isEnemyBishopOrQueenOrKing) {
                return false;
            }
        }

        var horizontalAndVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, futureKing, previousKing);

        for (Field field : horizontalAndVerticalFields) {
            Piece piece = field.pieceOptional().orElseThrow();

            boolean isEnemyRookOrQueenOrKing = (piece instanceof Rook || piece instanceof Queen || piece instanceof King)
                    && !piece.color().equals(color);

            if (isEnemyRookOrQueenOrKing) {
                return false;
            }
        }

        return true;
    }

    private boolean validatePieceMovementForKingSafety(ChessBoard chessBoard, Coordinate kingPosition,
                                                       Coordinate from, Coordinate to) {
        var diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, kingPosition, from, to);

        for (Field field : diagonalFields) {
            Piece piece = field.pieceOptional().orElseThrow();

            boolean isEnemyBishopOrQueen = (piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color);

            if (isEnemyBishopOrQueen) {
                return false;
            }
        }

        var horizontalAndVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, kingPosition);

        for (Field field : horizontalAndVerticalFields) {
            Piece piece = field.pieceOptional().orElseThrow();

            boolean isEnemyRookOrQueen = (piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color);

            if (isEnemyRookOrQueen) {
                return false;
            }
        }

        return true;
    }

    private List<Field> knightAttackPositions(ChessBoard chessBoard, Coordinate pivot) {
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
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private List<Field> pawnsThreateningCoordinate(ChessBoard chessBoard, Coordinate pivot, Color color) {
        List<StatusPair<Coordinate>> coordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
          coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() - 1));
          coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() + 1));
        } else {
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() + 1));
        }

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private List<Field> coordinatesThreatenedByPawn(ChessBoard chessBoard, Coordinate pivot, Color color) {
        List<StatusPair<Coordinate>> coordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() + 1));
        } else {
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() + 1));
        }

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot) {
        int row = pivot.getRow();
        char column = pivot.getColumn();

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
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private boolean fieldIsBlockedOrDangerous(ChessBoard chessBoard, Field field) {
        if (field.pieceOptional().orElseThrow().color().equals(color)) {
            return true;
        }

//        return validateKingMovementForSafety(chessBoard, field.getCoordinate());
        return false;
    }

    private boolean pawnMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = coordinatesThreatenedByPawn(chessBoard, to, color);

        for (Field possibleKing : possibleKings) {
            if (possibleKing.getCoordinate().equals(king)) {
                return true;
            }
        }

        return validateDirections(chessBoard, king, from, to, Pawn.class);
    }

    private boolean knightMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = knightAttackPositions(chessBoard, to);

        for (Field possibleKing : possibleKings) {
            if (possibleKing.getCoordinate().equals(king)) {
                return true;
            }
        }

        return validateDirections(chessBoard, king, from, to, Knight.class);
    }

    private boolean validateDirections(ChessBoard chessBoard, Coordinate king,
                                       Coordinate from, Coordinate to,
                                       Class<? extends Piece> clazz) {
        var diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, king, to, oppositePiece(clazz));

        for (Field field : diagonalFields) {
            if (field.getCoordinate().equals(from)) {
                continue;
            }

            Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color)) {
                return true;
            }
        }

        var horizontalAndVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, king, to, oppositePiece(clazz));

        for (Field field : horizontalAndVerticalFields) {
            if (field.getCoordinate().equals(from)) {
                continue;
            }

            Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color)) {
                return true;
            }
        }

        return false;
    }

    private Coordinate getOurKing(ChessBoard board) {
        if (this.color.equals(Color.WHITE)) {
            return board.currentWhiteKingPosition();
        } else {
            return board.currentBlackKingPosition();
        }
    }

    private Piece oppositePiece(Class<? extends Piece> type) {
        Color oppositeColor = color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;

        try {
            return type.getDeclaredConstructor(Color.class).newInstance(oppositeColor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create opposite piece", e);
        }
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

        public Optional<Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                Field field = chessBoard.field(coordinate);
                if (field.isPresent()) {
                    return Optional.of(field);
                }

                possibleCoordinate = strategy.apply(coordinate);
            }

            return Optional.empty();
        }

        public Optional<Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot, Coordinate ignore) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(ignore)) {
                    continue;
                }

                Field field = chessBoard.field(coordinate);
                if (field.isPresent()) {
                    return Optional.of(field);
                }

                possibleCoordinate = strategy.apply(coordinate);
            }

            return Optional.empty();
        }

        public Optional<Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                 Coordinate replace, Piece replacement) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(replace)) {
                    Field field = new Field(replace, replacement);
                    return Optional.of(field);
                }

                Field field = chessBoard.field(coordinate);
                if (field.isPresent()) {
                    return Optional.of(field);
                }

                possibleCoordinate = strategy.apply(coordinate);
            }

            return Optional.empty();
        }

        public Optional<Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot,
                                                 Coordinate ignore, Coordinate replace) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(ignore)) {
                    continue;
                }

                if (coordinate.equals(replace)) {
                    return Optional.empty();
                }

                Field field = chessBoard.field(coordinate);
                if (field.isPresent()) {
                    return Optional.of(field);
                }

                possibleCoordinate = strategy.apply(coordinate);
            }

            return Optional.empty();
        }

        public static List<Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot) {
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

        public static List<Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
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

        public static List<Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
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

        public static List<Field> occupiedFieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot,
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

        public static List<Field> occupiedFieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot,
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

        public static List<Field> occupiedFieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot, Coordinate ignore) {
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


        public static List<Field> occupiedFieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
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

        public static List<Piece> fieldsFromAllDirections(ChessBoard chessBoard, Coordinate pivot, Coordinate imaginary) {
            var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, imaginary);
            var top = TOP.occupiedFieldFrom(chessBoard, pivot, imaginary);
            var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, imaginary);

            var left = LEFT.occupiedFieldFrom(chessBoard, pivot, imaginary);
            var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, imaginary);

            var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, imaginary);
            var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, imaginary);
            var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, imaginary);

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
                    .map(field -> field.pieceOptional().orElseThrow())
                    .toList();
        }

        public static List<Field> fieldsFromAllDirections(ChessBoard chessBoard, Coordinate pivot) {
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
    }
}