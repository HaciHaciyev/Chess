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

        if (!(startField.pieceOptional().get() instanceof King(var kingColor))) {
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
            if (chessBoard.isCastling(chessBoard.theKing(color), from, to)) {
                return safeToCastle(chessBoard, from, to);
            }

            return validateKingMovementForSafety(chessBoard, from, to);
        }

        return validatePieceMovementForKingSafety(chessBoard, kingPosition, from, to);
    }

    private boolean safeToCastle(ChessBoard chessBoard, Coordinate presentKing, Coordinate futureKing) {
        List<Field> fieldsToCastle = getCastLingFields(chessBoard, presentKing, futureKing);

        for (Field field : fieldsToCastle) {
            if (field.isPresent() && !field.getCoordinate().equals(futureKing)) {
                return false;
            }

            var pawns = pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), color);

            for (Field pawn : pawns) {
                if (pawn.pieceOptional().get() instanceof Pawn) {
                    return false;
                }
            }

            var knights = knightAttackPositions(chessBoard, field.getCoordinate());

            for (Field knight : knights) {
                Piece piece = knight.pieceOptional().get();
                if (piece instanceof Knight && !piece.color().equals(color)) {
                    return false;
                }
            }

            var diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, field.getCoordinate());

            for (Field diagonalField : diagonalFields) {
                Piece piece = diagonalField.pieceOptional().get();

                if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color)) {
                    return false;
                }
            }

            var horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, field.getCoordinate());

            for (Field horizontalField : horizontalVerticalFields) {
                Piece piece = horizontalField.pieceOptional().get();

                if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color)) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Field> getCastLingFields(ChessBoard chessBoard, Coordinate presentKing, Coordinate futureKing) {
        var from = presentKing.getColumn();
        var to = futureKing.getColumn();

        List<Field> fields = new ArrayList<>();
        fields.add(chessBoard.field(presentKing));

        // short castling
        if (from > to) {
            while (true) {
                var left = Coordinate.coordinate(presentKing.getRow(), presentKing.getColumn() - 1).valueOrElseThrow();

                if (left.equals(futureKing)) {
                    fields.add(chessBoard.field(left));
                    return fields;
                }
                fields.add(chessBoard.field(left));
            }
        }

        // long castling
        if (from < to) {
            while (true) {
                var right = Coordinate.coordinate(presentKing.getRow(), presentKing.getColumn() + 1).valueOrElseThrow();

                if (right.equals(futureKing)) {
                    fields.add(chessBoard.field(right));
                    return fields;
                }
                fields.add(chessBoard.field(right));
            }
        }

        return fields;
    }

    public boolean check(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        Coordinate king = getKingCoordinate(chessBoard);

        return switch (piece) {
            case Pawn _ -> !pawnMovedCheck(chessBoard, king, from, to).isEmpty();
            case Knight _ -> !knightMovedCheck(chessBoard, king, from, to).isEmpty();
            case Bishop _ -> !validateDirectionsCheck(chessBoard, king, from, to, Bishop.class).isEmpty();
            case Rook _ -> !validateDirectionsCheck(chessBoard, king, from, to, Rook.class).isEmpty();
            case Queen _ -> !validateDirectionsCheck(chessBoard, king, from, to, Queen.class).isEmpty();
            case King _ -> !validateDirectionsCheck(chessBoard, king, from, to, King.class).isEmpty();
        };
    }

    public boolean checkmate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        Coordinate king = getKingCoordinate(chessBoard);

        return switch (piece) {
            case Pawn _ -> pawnMovedCheckmate(chessBoard, king, from, to);
            case Knight _ -> knightMovedCheckmate(chessBoard, king, from, to);
            case Bishop _ -> otherMovedCheckmate(chessBoard, king, from, to, Bishop.class);
            case Rook _ -> otherMovedCheckmate(chessBoard, king, from, to, Rook.class);
            case Queen _ -> otherMovedCheckmate(chessBoard, king, from, to, Queen.class);
            case King _ -> otherMovedCheckmate(chessBoard, king, from, to, King.class);
        };
    }

    public boolean stalemate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Coordinate king = getKingCoordinate(chessBoard);
        boolean check = check(chessBoard, from, to);

        if (check) {
            return false;
        }

        List<Field> surroundings = surroundingFields(chessBoard, king);

        boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));

        if (!surrounded) {
            return false;
        }

        List<Field> fields = getAllFriendlyFields(chessBoard);

        for (Field field : fields) {
            Piece piece = field.pieceOptional().get();
            Coordinate coordinate = field.getCoordinate();

            if (piece instanceof Pawn) {
                List<Field> coords = coordinatesThreatenedByPawn(chessBoard, coordinate, color);

                for (Field coord : coords) {
                    if (piece.isValidMove(chessBoard, coordinate, coord.getCoordinate()).status()) {
                        return false;
                    }
                }
            }
            if (piece instanceof Knight) {
                List<Field> coords = knightAttackPositions(chessBoard, king);

                for (Field coord : coords) {
                    if (piece.isValidMove(chessBoard, coordinate, coord.getCoordinate()).status()) {
                        return false;
                    }
                }
            }
            if (piece instanceof Bishop) {
                List<Field> coords = Direction.fieldsFromDiagonalDirections(chessBoard, coordinate);

                for (Field coord : coords) {
                    if (piece.isValidMove(chessBoard, coordinate, coord.getCoordinate()).status()) {
                        return false;
                    }
                }
            }
            if (piece instanceof Rook) {
                List<Field> coords = Direction.fieldsFromHorizontalAndVerticalDirections(chessBoard, coordinate);

                for (Field coord : coords) {
                    if (piece.isValidMove(chessBoard, coordinate, coord.getCoordinate()).status()) {
                        return false;
                    }
                }
            }
            if (piece instanceof Queen) {
                List<Field> coords = Direction.fieldsFromAllDirections(chessBoard, coordinate);

                for (Field coord : coords) {
                    if (piece.isValidMove(chessBoard, coordinate, coord.getCoordinate()).status()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean otherMovedCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to, Class<? extends Piece> clazz) {
        List<Field> enemies = validateDirectionsCheck(chessBoard, king, from, to, clazz);

        if (enemies.isEmpty()) {
            return false;
        }

        List<Field> surroundings = surroundingFields(chessBoard, king, enemies);

        if (enemies.size() > 1) {
            return surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        }

        Field enemyField = enemies.getFirst();
        Piece enemy = enemyField.pieceOptional().get();

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {
            Direction direction = getEnemyDirection(king, enemyField.getCoordinate());

            List<Field> fields = direction.fieldsUntil(chessBoard, king, enemyField.getCoordinate());

            boolean block = canBlock(chessBoard, fields, king);
            boolean eat = canEat(chessBoard, king, enemyField);

            return block || eat;
        }

        return false;
    }

    private boolean knightMovedCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        List<Field> enemies = knightMovedCheck(chessBoard, king, from, to);

        if (enemies.isEmpty()) {
            return false;
        }

        List<Field> surroundings = surroundingFields(chessBoard, king, enemies);

        if (enemies.size() > 1) {
            return surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        }

        Field enemyField = enemies.getFirst();
        Piece enemy = enemyField.pieceOptional().get();

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {
            Direction direction = getEnemyDirection(king, enemyField.getCoordinate());

            List<Field> fields = direction.fieldsUntil(chessBoard, king, enemyField.getCoordinate());

            boolean block = canBlock(chessBoard, fields, king);
            boolean eat = canEat(chessBoard, king, enemyField);

            return block || eat;
        }

        return false;
    }

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot, List<Field> replace) {
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
                .map(field -> {
                    for (Field toReplace : replace) {
                        if (field.getCoordinate().equals(toReplace.getCoordinate())) {
                            return toReplace;
                        }
                    }
                    return field;
                })
                .toList();
    }

    private boolean pawnMovedCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        List<Field> enemies = pawnMovedCheck(chessBoard, king, from, to);

        if (enemies.size() != 1) {
            return false;
        }

        List<Field> surroundings = surroundingFields(chessBoard, king, enemies.getFirst());

        boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));

        if (!surrounded) {
            return false;
        }

        Field enemyField = enemies.getFirst();

        Piece enemy = enemyField.pieceOptional().get();
        if (enemy instanceof Pawn) {
            return true;
        }

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {
            Direction direction = getEnemyDirection(king, enemyField.getCoordinate());

            List<Field> fields = direction.fieldsUntil(chessBoard, king, enemyField.getCoordinate());

            boolean block = canBlock(chessBoard, fields, king);
            boolean eat = canEat(chessBoard, king, enemyField);

            return block || eat;
        }

        return false;
    }

    private boolean canEat(ChessBoard chessBoard, Coordinate king, Field enemyField) {
        List<Field> possiblePawns = pawnsThreateningCoordinate(chessBoard, enemyField.getCoordinate(), color);

        for (Field possiblePawn : possiblePawns) {
            if (safeForKing(chessBoard, king, possiblePawn.getCoordinate(), enemyField.getCoordinate())) {
                return true;
            }
        }

        List<Field> knights = knightAttackPositions(chessBoard, enemyField.getCoordinate());

        for (Field knight : knights) {
            if (knight.pieceOptional().get().color().equals(color)) {
                return safeForKing(chessBoard, king, knight.getCoordinate(), enemyField.getCoordinate());
            }
        }

        List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, enemyField.getCoordinate());

        for (Field diagonalField : diagonalFields) {
            Piece piece = diagonalField.pieceOptional().get();

            if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(color)) {
                return safeForKing(chessBoard, king, diagonalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, enemyField.getCoordinate());

        for (Field horizontalVerticalField : horizontalVertical) {
            Piece piece = horizontalVerticalField.pieceOptional().get();

            if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(color)) {
                return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        return false;
    }

    private boolean canBlock(ChessBoard chessBoard, List<Field> fields, Coordinate king) {
        for (Field field : fields) {

            Coordinate currentCoordinate = field.getCoordinate();
            StatusPair<Coordinate> possibleCoordinate;

            if (Color.WHITE.equals(color)) {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() - 1, currentCoordinate.getColumn());
            } else {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() + 1, currentCoordinate.getColumn());
            }

            if (possibleCoordinate.status()) {
                Coordinate pawnCoordinate = possibleCoordinate.valueOrElseThrow();
                Field possiblePawn = chessBoard.field(pawnCoordinate);

                if (possiblePawn.isPresent() && possiblePawn.pieceOptional().get().color().equals(color)) {
                    return safeForKing(chessBoard, king, pawnCoordinate, currentCoordinate);
                }
            }

            List<Field> knights = knightAttackPositions(chessBoard, currentCoordinate);

            for (Field knight : knights) {
                if (knight.pieceOptional().get().color().equals(color)) {
                    return safeForKing(chessBoard, king, knight.getCoordinate(), currentCoordinate);
                }
            }

            List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, currentCoordinate);

            for (Field diagonalField : diagonalFields) {
                Piece piece = diagonalField.pieceOptional().get();

                if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(color)) {
                    return safeForKing(chessBoard, king, diagonalField.getCoordinate(), currentCoordinate);
                }
            }

            List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, currentCoordinate);

            for (Field horizontalVerticalField : horizontalVertical) {
                Piece piece = horizontalVerticalField.pieceOptional().get();

                if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(color)) {
                    return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), currentCoordinate);
                }
            }
        }
        return false;
    }

    private Direction getEnemyDirection(Coordinate from, Coordinate to) {
        if (to.getColumn() > from.getColumn() && to.getRow() > from.getRow()) {
            return Direction.TOP_RIGHT;
        } else if (to.getColumn() > from.getColumn() && to.getRow() < from.getRow()) {
            return Direction.BOTTOM_RIGHT;
        } else if (to.getColumn() < from.getColumn() && to.getRow() > from.getRow()) {
            return Direction.TOP_LEFT;
        } else {
            return Direction.BOTTOM_LEFT;
        }
    }

    private List<Field> getAllFriendlyFields(ChessBoard chessBoard) {
        Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates).map(chessBoard::field)
                .filter(Field::isPresent)
                .filter(field -> field.pieceOptional().get().color().equals(color))
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
                .toList();
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
                .filter(field -> field.pieceOptional().get() instanceof Pawn)
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

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot, Field toReplace) {
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
                .map(field -> {
                    if (toReplace != null && field.getCoordinate().equals(toReplace.getCoordinate())) {
                        return toReplace;
                    }
                    return field;
                })
                .toList();
    }

    private boolean fieldIsBlockedOrDangerous(ChessBoard chessBoard, Field field) {
        if (field.isPresent() && field.pieceOptional().get().color().equals(color)) {
            return true;
        }

        List<Field> pawns = pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), color);

        if (!pawns.isEmpty()) {
            return true;
        }

        List<Field> knights = knightAttackPositions(chessBoard, field.getCoordinate());

        for (Field possibleKnight : knights) {
            Piece piece = possibleKnight.pieceOptional().get();
            if (piece instanceof Knight && !piece.color().equals(color)) {
                return true;
            }
        }

        List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, field.getCoordinate());

        for (Field diagonalField : diagonalFields) {
            Piece piece = diagonalField.pieceOptional().get();

            if (piece instanceof Bishop && !piece.color().equals(color)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(color)) {
                return true;
            }
        }

        List<Field> horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, field.getCoordinate());

        for (Field horizontalVerticalField : horizontalVerticalFields) {
            Piece piece = horizontalVerticalField.pieceOptional().get();

            if (piece instanceof Rook && !piece.color().equals(color)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(color)) {
                return true;
            }
        }

        return false;
    }

    private List<Field> pawnMovedCheck(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = coordinatesThreatenedByPawn(chessBoard, to, color);
        List<Field> checkFields = new ArrayList<>();

        for (Field possibleKing : possibleKings) {
            if (possibleKing.getCoordinate().equals(king)) {
                checkFields.add(new Field(to, oppositePiece(Pawn.class)));
            }
        }

        List<Field> checksFromOtherDirections = validateDirectionsCheck(chessBoard, king, from, to, Pawn.class);

        if (!checksFromOtherDirections.isEmpty()) {
            checkFields.addAll(checksFromOtherDirections);
            return checkFields;
        }

        return checkFields;
    }

    private List<Field> knightMovedCheck(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = knightAttackPositions(chessBoard, to);
        List<Field> checkFields = new ArrayList<>();

        for (Field possibleKing : possibleKings) {
            if (possibleKing.getCoordinate().equals(king)) {
                checkFields.add(new Field(to, oppositePiece(Knight.class)));
            }
        }

        List<Field> checksFromOtherDirections = validateDirectionsCheck(chessBoard, king, from, to, Knight.class);

        if (!checksFromOtherDirections.isEmpty()) {
            checkFields.addAll(checksFromOtherDirections);
            return checkFields;
        }

        return checkFields;
    }

    private List<Field> validateDirectionsCheck(ChessBoard chessBoard, Coordinate king,
                                                Coordinate from, Coordinate to,
                                                Class<? extends Piece> clazz) {
        var diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, king, to, oppositePiece(clazz));
        List<Field> checkFields = new ArrayList<>();

        for (Field field : diagonalFields) {
            if (field.getCoordinate().equals(from)) {
                continue;
            }

            Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color)) {
                checkFields.add(new Field(to, piece));
            }
        }

        var horizontalAndVerticalFields = Direction.occupiedFieldsFromHorizontalAndVerticalDirections(chessBoard, king, to, oppositePiece(clazz));

        for (Field field : horizontalAndVerticalFields) {
            if (field.getCoordinate().equals(from)) {
                continue;
            }

            Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color)) {
                checkFields.add(new Field(to, piece));
            }
        }

        return checkFields;
    }

    private Coordinate getKingCoordinate(ChessBoard board) {
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

        public static List<Field> fieldsFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot) {
            var topLeft = TOP_LEFT.fieldsFrom(chessBoard, pivot);
            var topRight = TOP_RIGHT.fieldsFrom(chessBoard, pivot);
            var bottomLeft = BOTTOM_LEFT.fieldsFrom(chessBoard, pivot);
            var bottomRight = BOTTOM_RIGHT.fieldsFrom(chessBoard, pivot);

            topLeft.addAll(topRight);
            topLeft.addAll(bottomLeft);
            topLeft.addAll(bottomRight);

            return topLeft;
        }

        public static List<Field> fieldsFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
            var top = TOP.fieldsFrom(chessBoard, pivot);
            var left = LEFT.fieldsFrom(chessBoard, pivot);
            var right = RIGHT.fieldsFrom(chessBoard, pivot);
            var bottom = BOTTOM.fieldsFrom(chessBoard, pivot);

            top.addAll(left);
            top.addAll(right);
            top.addAll(bottom);

            return top;
        }

        public List<Field> fieldsUntil(ChessBoard chessBoard, Coordinate pivot, Coordinate end) {
            var possibleCoordinate = strategy.apply(pivot);

            List<Field> fields = new ArrayList<>();

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(end)) {
                    break;
                }

                fields.add(chessBoard.field(coordinate));


                possibleCoordinate = strategy.apply(coordinate);
            }

            return fields;
        }

        public List<Field> fieldsFrom(ChessBoard chessBoard, Coordinate pivot) {
            var possibleCoordinate = strategy.apply(pivot);
            List<Field> fields = new ArrayList<>();

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();
                fields.add(chessBoard.field(coordinate));

                possibleCoordinate = strategy.apply(coordinate);
            }

            return fields;
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
    }
}