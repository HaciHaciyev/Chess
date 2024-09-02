package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Pawn;
import core.project.chess.domain.aggregates.chess.pieces.Piece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ChessBoardUtils {

    private ChessBoardUtils() {}

    public static Coordinate getKingCoordinate(ChessBoard board, Color color) {
        if (color.equals(Color.WHITE)) {
            return board.currentWhiteKingPosition();
        } else {
            return board.currentBlackKingPosition();
        }
    }

    public static List<ChessBoard.Field> getAllFriendlyFields(ChessBoard chessBoard, Coordinate ignore, Color color) {
        final Coordinate kingCoordinate = getKingCoordinate(chessBoard, color);
        final Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates)
                .map(chessBoard::field)
                .filter(ChessBoard.Field::isPresent)
                .filter(field -> !field.getCoordinate().equals(kingCoordinate))
                .filter(field -> !field.getCoordinate().equals(ignore))
                .filter(field -> field.pieceOptional().orElseThrow().color().equals(color))
                .toList();
    }

    // TODO WIP
    public static List<ChessBoard.Field> getAllFriendlyFields(ChessBoard chessBoard, Color color, boolean includingKing) {
        final Coordinate kingCoordinate = getKingCoordinate(chessBoard, color);

        List<ChessBoard.Field> fields = getAllFriendlyFields(chessBoard, color);

        if (includingKing) {
            return fields;
        }

        fields.removeIf(field -> field.getCoordinate().equals(kingCoordinate));

        return fields;
    }

    private static List<ChessBoard.Field> getAllFriendlyFields(ChessBoard chessBoard, Color color) {
        final Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates)
                .map(chessBoard::field)
                .filter(ChessBoard.Field::isPresent)
                .filter(field -> field.pieceOptional().orElseThrow().color().equals(color))
                .toList();
    }

    public static List<ChessBoard.Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot) {
        int row = pivot.getRow();
        int column = pivot.getColumnAsInt();

        int[][] directions = {
                {1, 0}, {-1, 0}, {0, -1}, {0, 1},   // up, down, left, right
                {1, -1}, {1, 1}, {-1, -1}, {-1, 1}  // upper-left, upper-right, down-left, down-right
        };

        List<ChessBoard.Field> list = new ArrayList<>();

        for (int[] direction : directions) {
            var possibleCoordinate = Coordinate.coordinate(row + direction[0], column + direction[1]);

            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = chessBoard.field(coordinate);
                list.add(field);
            }
        }

        return list;
    }


    public static List<ChessBoard.Field> surroundingFields(ChessBoard chessBoard,
                                                           Coordinate pivot,
                                                           UnaryOperator<ChessBoard.Field> mapping) {
        var surroundings = surroundingFields(chessBoard, pivot);
        surroundings.replaceAll(mapping);

        return surroundings;
    }

    public static List<ChessBoard.Field> coordinatesThreatenedByPawn(ChessBoard chessBoard, Coordinate pivot, Color color) {
        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() + 1));
        }

        List<ChessBoard.Field> fields = new ArrayList<>();
        for (var possibleCoordinate : possibleCoordinates) {
            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = chessBoard.field(coordinate);
                fields.add(field);
            }
        }

        return fields;
    }

    public static List<ChessBoard.Field> pawnsThreateningCoordinate(ChessBoard chessBoard, Coordinate pivot, Color color) {
        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() + 1));
        }

        List<ChessBoard.Field> fields = new ArrayList<>();
        for (var possibleCoordinate : possibleCoordinates) {
            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                var field = chessBoard.field(coordinate);
                if (field.isPresent() && field.pieceOptional().orElseThrow() instanceof Pawn) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    public static List<ChessBoard.Field> knightAttackPositions(ChessBoard chessBoard, Coordinate pivot) {
        int row = pivot.getRow();
        int col = pivot.getColumnAsInt();

        int[][] moves = {
                {1, -2}, {2, -1},   // top-left
                {2, 1}, {1, 2},     // top-right
                {-1, 2}, {-2, 1},   // bottom-right
                {-2, -1}, {-1, -2}  // bottom-left
        };

        List<ChessBoard.Field> fields = new ArrayList<>();

        for (int[] move : moves) {
            var possibleCoordinate = Coordinate.coordinate(row + move[0], col + move[1]);

            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = chessBoard.field(coordinate);

                if (field.isPresent()) {
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    public static List<ChessBoard.Field> getCastlingFields(final ChessBoard chessBoard,
                                                           final Coordinate presentKing,
                                                           final Coordinate futureKingPosition) {
        final char from = presentKing.getColumn();
        final char to = futureKingPosition.getColumn();

        final List<ChessBoard.Field> fields = new ArrayList<>();
        fields.add(chessBoard.field(presentKing));

        int direction = from < to ? 1 : -1;
        addCastlingFields(chessBoard, presentKing, futureKingPosition, fields, direction);

        return fields;
    }

    private static void addCastlingFields(ChessBoard chessBoard,
                                          Coordinate presentKing,
                                          Coordinate futureKingPosition,
                                          List<ChessBoard.Field> fields,
                                          int direction) {
        int row = presentKing.getRow();
        int column = presentKing.getColumnAsInt() + direction;

        while (true) {
            final Coordinate coordinate = Coordinate
                    .coordinate(row, column)
                    .orElseThrow(() -> new IllegalStateException("Can't create coordinate. The method needs repair."));

            fields.add(chessBoard.field(coordinate));

            if (coordinate.equals(futureKingPosition)) {
                return;
            }

            column += direction;
        }
    }

    public static Optional<ChessBoard.Field> getForwardField(ChessBoard chessBoard, Coordinate coordinate) {
        ChessBoard.Field field = chessBoard.field(coordinate);
        Piece piece = field.pieceOptional().orElseThrow();

        int direction = piece.color().equals(Color.WHITE) ? 1 : -1;
        final var possibleForwardCoordinate = Coordinate.coordinate(coordinate.getRow() + direction, coordinate.getColumnAsInt());

        if (possibleForwardCoordinate.status()) {
            Coordinate forward = possibleForwardCoordinate.orElseThrow();
            ChessBoard.Field forwardField = chessBoard.field(forward);

            return Optional.of(forwardField);
        }

        return Optional.empty();
    }

    public static Optional<ChessBoard.Field> getForwardField(ChessBoard chessBoard,
                                                             Coordinate coordinate,
                                                             Predicate<ChessBoard.Field> predicate) {
        Optional<ChessBoard.Field> forward = getForwardField(chessBoard, coordinate);

        if (forward.isPresent() && predicate.test(forward.get())) {
            return forward;
        }

        return Optional.empty();
    }
}
