package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        var knights = knightAttackPositions(chessBoard, kingPosition);

        if (knights.isEmpty()) {
            return true;
        }

        boolean isNotKnight = knights.stream().noneMatch(field -> field.pieceOptional().orElseThrow() instanceof Knight);

        boolean isEaten = knights.stream()
                .filter(field -> field.pieceOptional().orElseThrow() instanceof Knight)
                .allMatch(field -> field.getCoordinate().equals(to));

        return isNotKnight || isEaten;
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


    private boolean fromPawns(ChessBoard chessBoard, Coordinate kingPosition, Coordinate to) {
        var pawns = pawnsThreateningCoordinate(chessBoard, kingPosition);

        if (pawns.isEmpty()) {
            return true;
        }

        boolean isNotPawn = pawns.stream().noneMatch(pawn -> pawn.pieceOptional().orElseThrow() instanceof Pawn);

        boolean isEaten = pawns.stream().allMatch(pawn -> pawn.getCoordinate().equals(to));

        boolean isFriendly = pawns.stream().allMatch(pawn -> pawn.pieceOptional().orElseThrow().color().equals(color));

        return isNotPawn || isEaten || isFriendly;
    }

    private List<Field> pawnsThreateningCoordinate(ChessBoard chessBoard, Coordinate pivot) {
        return Stream.of(
                        Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() - 1),
                        Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() + 1)
                )
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private List<Field> coordinatesThreatenedByPawn(ChessBoard chessBoard, Coordinate pawn) {
        return Stream.of(
                        Coordinate.coordinate(pawn.getRow() + 1, pawn.getColumn() - 1),
                        Coordinate.coordinate(pawn.getRow() + 1, pawn.getColumn() + 1)
                )
                .filter(StatusPair::status)
                .map(StatusPair::valueOrElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private boolean fromAnythingElse(ChessBoard chessBoard, Coordinate kingPosition, Coordinate imaginary) {
        var pieces = Direction.piecesFromAllDirections(chessBoard, kingPosition, imaginary);

        if (pieces.isEmpty()) {
            return true;
        }

        boolean areEnemiesButCantHurt = pieces.stream()
                .filter(piece -> !piece.color().equals(color))
                .noneMatch(piece -> piece instanceof Bishop
                        || piece instanceof Rook
                        || piece instanceof Queen);

        boolean areFriendly = pieces.stream()
                .allMatch(piece -> piece.color().equals(color));

        return areEnemiesButCantHurt || areFriendly;
    }

    // no use yet
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


    public boolean check(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        Coordinate king = getKing(chessBoard);

        return switch (piece) {
            case Pawn _ -> pawnMoved(chessBoard, king, from, to);
            case Knight _ -> knightMoved(chessBoard, king, from, to);
            case Bishop _ -> bishopMoved(chessBoard, king, from, to);
            case Rook _ -> rookMoved(chessBoard, king, from, to);
            case Queen _ -> queenMoved(chessBoard, king, from, to);
            case King _ -> kingMoved(chessBoard, king, from, to);
        };
    }

    private boolean pawnMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = coordinatesThreatenedByPawn(chessBoard, to);

        boolean checkFromPawn = possibleKings.stream()
                .anyMatch(field -> field.getCoordinate().equals(king));

        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(Pawn.class));

        boolean checkFromAnythingElse = enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));

        return checkFromPawn || checkFromAnythingElse;
    }

    private boolean knightMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var possibleKings = knightAttackPositions(chessBoard, to);

        boolean checkFromKnight = possibleKings.stream()
                .anyMatch(field -> field.getCoordinate().equals(king));

        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(Knight.class));

        boolean checkFromAnythingElse = enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));

        return checkFromKnight || checkFromAnythingElse;
    }

    private boolean bishopMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var pieces = Direction.piecesFromDiagonalDirections(chessBoard, to);

        boolean checkFromBishop = pieces.stream()
                .filter(King.class::isInstance)
                .anyMatch(opponentKing -> opponentKing.color().equals(color));

        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(Bishop.class));

        boolean checkFromAnythingElse = enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));

        return checkFromBishop || checkFromAnythingElse;
    }

    private boolean rookMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var pieces = Direction.piecesFromHorizontalAndVerticalDirections(chessBoard, to);

        boolean checkFromRook = pieces.stream()
                .filter(King.class::isInstance)
                .anyMatch(opponentKing -> opponentKing.color().equals(color));

        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(Rook.class));

        boolean checkFromAnythingElse = enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));

        return checkFromRook || checkFromAnythingElse;
    }

    private boolean queenMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var pieces = Direction.piecesFromAllDirections(chessBoard, to);

        boolean checkFromQueen = pieces.stream()
                .filter(King.class::isInstance)
                .anyMatch(opponentKing -> opponentKing.color().equals(color));

        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(Queen.class));

        boolean checkFromAnythingElse = enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));

        return checkFromQueen || checkFromAnythingElse;
    }

    private boolean kingMoved(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        var enemyPieces = Direction.enemyPiecesFromAllDirections(chessBoard, king, from, to, oppositePiece(King.class));

        return enemyPieces.stream()
                .noneMatch(piece -> (piece instanceof Pawn || piece instanceof Knight || piece instanceof King)
                        && !piece.color().equals(color));
    }

    private Coordinate getKing(ChessBoard board) {
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

        /**
         * Returns an Optional occupied field towards specified direction from pivot.
         * <p>
         * If field's coordinate equals imaginary piece's coordinate then field with mock friendly piece is returned
         */
        public Optional<Field> occupiedFieldFrom(ChessBoard chessBoard, Coordinate pivot, Coordinate replace) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(replace)) {
                    return fieldWithMockPiece(chessBoard, pivot, replace);
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
                                                 Coordinate ignore, Coordinate replace,
                                                 Piece piece) {
            var possibleCoordinate = strategy.apply(pivot);

            while (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.valueOrElseThrow();

                if (coordinate.equals(ignore)) {
                    continue;
                }

                if (coordinate.equals(replace)) {
                    Field field = new Field(replace, piece);
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


        /**
         * Returns an Optional occupied field towards specified direction from pivot
         */
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

        public static List<Piece> enemyPiecesFromAllDirections(ChessBoard chessBoard, Coordinate pivot,
                                                               Coordinate from, Coordinate to,
                                                               Piece piece) {

            var topLeft = TOP_LEFT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);
            var top = TOP.occupiedFieldFrom(chessBoard, pivot, from, to, piece);
            var topRight = TOP_RIGHT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);


            var left = LEFT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);
            var right = RIGHT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);


            var bottomLeft = BOTTOM_LEFT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);
            var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot, from, to, piece);
            var bottomRight = BOTTOM_RIGHT.occupiedFieldFrom(chessBoard, pivot, from, to, piece);


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

        public static List<Piece> piecesFromAllDirections(ChessBoard chessBoard, Coordinate pivot, Coordinate imaginary) {
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

        public static List<Piece> piecesFromAllDirections(ChessBoard chessBoard, Coordinate pivot) {
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
                    .map(field -> field.pieceOptional().orElseThrow())
                    .toList();
        }


        public static List<Piece> piecesFromHorizontalAndVerticalDirections(ChessBoard chessBoard, Coordinate pivot) {
            var top = TOP.occupiedFieldFrom(chessBoard, pivot);
            var left = LEFT.occupiedFieldFrom(chessBoard, pivot);
            var right = RIGHT.occupiedFieldFrom(chessBoard, pivot);
            var bottom = BOTTOM.occupiedFieldFrom(chessBoard, pivot);

            return Stream.of(
                            top,
                            left,
                            right,
                            bottom
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::orElseThrow)
                    .map(field -> field.pieceOptional().orElseThrow())
                    .toList();
        }

        public static List<Piece> piecesFromDiagonalDirections(ChessBoard chessBoard, Coordinate pivot) {
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
                    .map(field -> field.pieceOptional().orElseThrow())
                    .toList();
        }


        private Optional<Field> fieldWithMockPiece(ChessBoard chessBoard, Coordinate pivot, Coordinate imaginary) {
            Color color = chessBoard.field(pivot).pieceOptional().orElseThrow().color();
            var mockPiece = new Pawn(color);
            var field = new Field(imaginary, mockPiece);
            return Optional.of(field);
        }

    }
}
