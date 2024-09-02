package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Pawn;
import core.project.chess.domain.aggregates.chess.pieces.Piece;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A utility class providing methods for various operations on a chessboard.
 *
 * <p>This class offers methods to determine the position of key pieces, identify threatened fields,
 * calculate potential moves, and other utility operations related to a chess game. It is designed to
 * work with the {@link ChessBoard} class and its associated entities such as {@link Coordinate},
 * {@link Color}, and {@link ChessBoard.Field}. The methods provided are static, as this class is intended
 * to be used as a collection of utility functions.</p>
 *
 * <p><strong>Note:</strong> All methods in this class are static, and the class itself is not intended to
 * be instantiated.</p>
 *
 * @see ChessBoard
 * @see Coordinate
 * @see Color
 * @see ChessBoard.Field
 */
public class ChessBoardUtils {

    private ChessBoardUtils() {
    }

    /**
     * Retrieves the coordinate of the King piece for the specified color on the given chess board.
     *
     * @param board The {@link ChessBoard} object representing the current state of the chess game.
     * @param color The {@link Color} enum value (WHITE or BLACK) indicating which King's position to retrieve.
     * @return The {@link Coordinate} object representing the position of the specified King on the board.
     * @throws NullPointerException if either the board or color parameter is null.
     * @see ChessBoard
     * @see Color
     * @see Coordinate
     */
    public static Coordinate getKingCoordinate(ChessBoard board, Color color) {
        Objects.requireNonNull(board);
        Objects.requireNonNull(color);

        if (color.equals(Color.WHITE)) {
            return board.currentWhiteKingPosition();
        } else {
            return board.currentBlackKingPosition();
        }
    }


    /**
     * Retrieves all fields on the chess board that contain pieces of the specified color
     * and satisfy the given predicate.
     *
     * <p>This method is an extension of the basic {@link #getAllFriendlyFields(ChessBoard, Color) getAllFriendlyFields} method.
     * It first finds all friendly fields, then applies a provided predicate to each field before returning the result.</p>
     *
     * <p>This method filters the chess board to find all fields that:</p>
     * <ol>
     *   <li>Contain a piece (are not empty).</li>
     *   <li>Contain a piece of the specified color.</li>
     *   <li>Satisfy the provided predicate.</li>
     * </ol>
     *
     * @param chessBoard The {@link  ChessBoard} object representing the current state of the chess game.
     * @param color      The {@link  Color} enum value (WHITE or BLACK) indicating which pieces to consider as friendly.
     * @param predicate  A {@link  Predicate<ChessBoard.Field>} used to further filter the fields.
     * @return A list of {@link  ChessBoard.Field} objects representing the fields that meet all the criteria.
     * @throws NullPointerException if any of the parameters {@code chessBoard}, {@code color}, or {@code predicate} is {@code null}.
     * @see ChessBoard
     * @see Color
     * @see ChessBoard.Field
     * @see Predicate
     */
    public static List<ChessBoard.Field> getAllFriendlyFields(ChessBoard chessBoard,
                                                              Color color,
                                                              Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(color);
        Objects.requireNonNull(predicate);

        List<ChessBoard.Field> fields = getAllFriendlyFields(chessBoard, color);

        return fields.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Retrieves all fields on the chess board that contain pieces of the specified color.
     *
     * <p>This method scans the entire chess board to find all fields that:</p>
     * <ol>
     *   <li>Contain a piece (are not empty).</li>
     *   <li>Contain a piece of the specified color.</li>
     * </ol>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param color      The {@link Color} enum value (WHITE or BLACK) indicating which pieces to consider as friendly.
     * @return A list of {@link ChessBoard.Field} objects representing the fields that contain friendly pieces.
     * @throws NullPointerException if either {@code chessBoard} or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Color
     * @see ChessBoard.Field
     */
    public static List<ChessBoard.Field> getAllFriendlyFields(ChessBoard chessBoard, Color color) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(color);

        final Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates)
                .map(chessBoard::field)
                .filter(ChessBoard.Field::isPresent)
                .filter(field -> field.pieceOptional().orElseThrow().color().equals(color))
                .toList();
    }

    /**
     * Retrieves all the surrounding fields of a given position on the chess board.
     *
     * <p>This method returns a list of all adjacent fields, including diagonals.
     * It considers all eight directions: up, down, left, right, and all four diagonals.
     * <b>Only fields that exist on the board are included in the result.</b></p>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param pivot      The {@link Coordinate} object representing the central position to find surrounding fields for.
     * @return A list of {@link ChessBoard.Field} objects representing all valid surrounding fields.
     * @throws NullPointerException if either {@code chessBoard} or {@code pivot} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public static List<ChessBoard.Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(pivot);

        int row = pivot.getRow();
        int column = pivot.columnToInt();

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

    /**
     * Retrieves and maps all the surrounding fields of a given position on the chess board.
     *
     * <p>This method is an extension of the basic {@link #surroundingFields(ChessBoard, Coordinate) surroundingFields} method.
     * It first finds all surrounding fields, then applies a provided predicate function to each field before returning the result.</p>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param pivot      The {@link Coordinate} object representing the central position to find surrounding fields for.
     * @param predicate    A {@link Predicate<ChessBoard.Field>} that will be applied to each surrounding field.
     * @return A list of {@link ChessBoard.Field} objects representing all valid surrounding fields, after applying the mapping function.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code mapping} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see UnaryOperator
     */
    public static List<ChessBoard.Field> surroundingFields(ChessBoard chessBoard,
                                                           Coordinate pivot,
                                                           Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(predicate);

        var surroundings = surroundingFields(chessBoard, pivot);
        surroundings.removeIf(predicate);

        return surroundings;
    }

    /**
     * Determines the fields that are threatened by a pawn at a given position on the chess board.
     *
     * <p>This method calculates the potential attack positions for a pawn of the specified color
     * from the given pivot position. For a white pawn, it considers the two diagonal squares
     * forward. For a black pawn, it considers the two diagonal squares backward.</p>
     *
     * <blockquote>Note that this method returns the threatened fields regardless of whether they are
     * occupied or not. It's the responsibility of the caller to filter out irrelevant fields
     * if necessary.</blockquote>
     *
     * @param chessBoard The {@code ChessBoard} object representing the current state of the chess game.
     * @param pivot      The {@code Coordinate} object representing the position of the pawn.
     * @param color      The {@code Color} enum value (WHITE or BLACK) indicating the color of the pawn.
     * @return A {@code List<ChessBoard.Field>} representing the fields threatened by the pawn.
     * This list may include fields that are empty.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see Color
     * @see ChessBoard.Field
     */
    public static List<ChessBoard.Field> coordinatesThreatenedByPawn(ChessBoard chessBoard, Coordinate pivot, Color color) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.columnToInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.columnToInt() + 1));
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

    /**
     * Determines the fields containing pawns that are threatening a given coordinate on the chess board.
     *
     * <p>This method calculates the positions from which pawns of the specified color could
     * potentially attack the given pivot coordinate. For white pawns, it checks the two
     * diagonal squares behind the pivot. For black pawns, it checks the two diagonal
     * squares in front of the pivot.</p>
     *
     * <p>The method only returns fields that:
     * <ol>
     *   <li>Exist on the board</li>
     *   <li>Contain a piece (are not empty)</li>
     *   <li>Contain a pawn of the specified color</li>
     * </ol>
     * </p>
     *
     * @param chessBoard The list of{@link ChessBoard} object representing the current state of the chess game.
     * @param pivot      The list of{@link Coordinate} object representing the position being threatened.
     * @param color      The list of{@link Color} enum value (WHITE or BLACK) indicating the color of the threatening pawns.
     * @return A list of{@link ChessBoard.Field} objects representing the fields containing pawns that threaten the pivot coordinate.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see Color
     * @see ChessBoard.Field
     * @see Pawn
     */
    public static List<ChessBoard.Field> pawnsThreateningCoordinate(ChessBoard chessBoard, Coordinate pivot, Color color) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.columnToInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.columnToInt() + 1));
        }

        List<ChessBoard.Field> fields = new ArrayList<>();
        for (var possibleCoordinate : possibleCoordinates) {
            if (!possibleCoordinate.status()) {
                continue;
            }

            Coordinate coordinate = possibleCoordinate.orElseThrow();
            var field = chessBoard.field(coordinate);

            if (field.isEmpty()) {
                continue;
            }

            Piece piece = field.pieceOptional().get();
            if (piece instanceof Pawn && piece.color().equals(color)) {
                fields.add(field);
            }
        }

        return fields;
    }

    /**
     * Determines all possible attack positions for a knight from a given coordinate on the chess board.
     *
     * <p>This method calculates all the positions that a knight could potentially move to or attack
     * from the given pivot position. A knight moves in an "L" shape: two squares in one direction
     * and then one square perpendicular to that direction.</p>
     *
     * <p>The method considers all eight possible knight moves:
     * <ul>
     *   <li>Two squares up and one left</li>
     *   <li>Two squares up and one right</li>
     *   <li>Two squares right and one up</li>
     *   <li>Two squares right and one down</li>
     *   <li>Two squares down and one right</li>
     *   <li>Two squares down and one left</li>
     *   <li>Two squares left and one up</li>
     *   <li>Two squares left and one down</li>
     * </ul>
     * </p>
     *
     * <p>Note that this method only returns fields that exist on the board and are occupied. Any potential
     * moves that would land outside the board's boundaries are automatically excluded.</p>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param pivot      The {@link Coordinate} object representing the starting position of the knight.
     * @return A list of{@link ChessBoard.Field} objects representing all valid positions a knight can attack from the given pivot.
     * @throws NullPointerException if either {@code chessBoard} or {@code pivot} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public static List<ChessBoard.Field> knightAttackPositions(ChessBoard chessBoard, Coordinate pivot) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(pivot);

        int row = pivot.getRow();
        int col = pivot.columnToInt();

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

    /**
     * Determines the fields involved in a castling move on the chess board.
     *
     * <p>This method calculates and returns a list of fields that are involved in a castling move,
     * given the current position of the king and its intended position after castling. The list
     * includes all fields that the king will pass through during the castling move, including
     * its starting and ending positions.</p>
     *
     * <p>Castling is a special move in chess where the king and a rook move simultaneously:
     * <ul>
     *   <li>The king moves two squares towards the rook.</li>
     *   <li>The rook moves to the square that the king crossed.</li>
     * </ul>
     * </p>
     *
     * <p>This method <strong>does not</strong> verify the legality of the castling move (e.g., checking for obstacles
     * or ensuring the king and rook have not moved before). It simply identifies the fields that
     * would be involved in the move.</p>
     *
     * @param chessBoard         The {@link ChessBoard} object representing the current state of the chess game.
     * @param presentKing        The {@link Coordinate} object representing the current position of the king.
     * @param futureKingPosition The {@link Coordinate} object representing the intended position of the king after castling.
     * @return A list of {@link ChessBoard.Field} objects representing all fields involved in the castling move, including the king's path.
     * @throws NullPointerException  if any of {@code chessBoard}, {@code presentKing}, or {@code futureKingPosition} is {@code null}.
     * @throws IllegalStateException if an invalid coordinate is encountered during calculation.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public static List<ChessBoard.Field> getCastlingFields(ChessBoard chessBoard,
                                                           Coordinate presentKing,
                                                           Coordinate futureKingPosition) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(presentKing);
        Objects.requireNonNull(futureKingPosition);

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
        int column = presentKing.columnToInt() + direction;

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

    /**
     * Retrieves the field directly in front of the piece located at the given coordinate on the chessboard.
     *
     * <p>The forward direction is determined based on the color of the piece at the given coordinate:
     * <ul>
     *   <li>White pieces move forward by increasing the row number.</li>
     *   <li>Black pieces move forward by decreasing the row number.</li>
     * </ul>
     *
     * <p>If the forward position is outside the board's boundaries, an empty {@link Optional} is returned.</p>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param coordinate The {@link Coordinate} of the piece whose forward field is to be determined.
     * @return An {@link Optional} containing the {@link ChessBoard.Field} directly in front of the specified piece,
     * or an empty {@link Optional} if the forward position is outside the board or invalid.
     * @throws NullPointerException if either the chessBoard or coordinate is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Optional
     */
    public static Optional<ChessBoard.Field> getForwardField(ChessBoard chessBoard, Coordinate coordinate) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(coordinate);

        ChessBoard.Field field = chessBoard.field(coordinate);
        Piece piece = field.pieceOptional().orElseThrow();

        int direction = piece.color().equals(Color.WHITE) ? 1 : -1;
        final var possibleForwardCoordinate = Coordinate.coordinate(coordinate.getRow() + direction, coordinate.columnToInt());

        if (possibleForwardCoordinate.status()) {
            Coordinate forward = possibleForwardCoordinate.orElseThrow();
            ChessBoard.Field forwardField = chessBoard.field(forward);

            return Optional.of(forwardField);
        }

        return Optional.empty();
    }

    /**
     * Retrieves the field directly in front of the piece located at the given coordinate on the chessboard,
     * if it satisfies the specified predicate.
     *
     * <p>The forward direction is determined based on the color of the piece at the given coordinate:
     * <ul>
     *   <li>White pieces move forward by increasing the row number.</li>
     *   <li>Black pieces move forward by decreasing the row number.</li>
     * </ul>
     *
     * <p>If the forward position is outside the board's boundaries or does not satisfy the predicate,
     * an empty {@link Optional} is returned.</p>
     *
     * @param chessBoard The {@link ChessBoard} object representing the current state of the chess game.
     * @param coordinate The {@link Coordinate} of the piece whose forward field is to be determined.
     * @param predicate  A {@link Predicate} used to test the forward field. If the field does not satisfy this predicate,
     *                   an empty {@link Optional} is returned.
     * @return An {@link Optional} containing the {@link ChessBoard.Field} directly in front of the specified piece
     * if it exists and satisfies the predicate, or an empty {@link Optional} otherwise.
     * @throws NullPointerException if any of the parameters (chessBoard, coordinate, or predicate) is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Predicate
     * @see Optional
     */
    public static Optional<ChessBoard.Field> getForwardField(ChessBoard chessBoard,
                                                             Coordinate coordinate,
                                                             Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(coordinate);
        Objects.requireNonNull(predicate);

        Optional<ChessBoard.Field> forward = getForwardField(chessBoard, coordinate);

        if (forward.isPresent() && predicate.test(forward.get())) {
            return forward;
        }

        return Optional.empty();
    }
}
