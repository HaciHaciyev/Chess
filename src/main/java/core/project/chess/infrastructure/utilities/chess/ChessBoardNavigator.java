package core.project.chess.infrastructure.utilities.chess;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.*;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import lombok.NonNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record ChessBoardNavigator(ChessBoard board) {


    public String prettyToString() {
        String fen = board.toString();
        int space = fen.indexOf(" ");

        String boardRepresentation = fen.substring(0, space);
        int turn = board.countOfMovement() + 1;
        String nextMove = fen.charAt(space + 1) == 'w' ? "White's turn" : "Black's turn";

        String whiteKingStatus = "NOT           ";
        String blackKingStatus = "NOT           ";

        String latestMovement;
        boolean movesMadePreviously = board.latestMovement().isPresent();

        if (movesMadePreviously) {
            latestMovement = board.latestMovement().map(pair -> pair.getFirst().name() + " -> " + pair.getSecond().name()).orElseThrow();
        } else {
            latestMovement = "...     ";
        }

        List<Piece> whiteCaptures = board.whiteCaptures();
        List<Piece> blackCaptures = board.blackCaptures();

        StringBuilder view = new StringBuilder();
        String[] rows = boardRepresentation.split("/");

        String ANSI_GREEN = "\u001B[32m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_DARK_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";

        view.append("   A  B  C  D  E  F  G  H\n");
        for (int row = 0, rowIndex = 8; row < rows.length; row++, rowIndex--) {
            String strRow = rows[row];
            view.append(rowIndex).append(" [");
            char[] charRow = strRow.toCharArray();

            for (int columnIndex = 0; columnIndex < charRow.length; columnIndex++) {
                char c = charRow[columnIndex];
                if (Character.isLetter(c)) {
                    String prettyPiece = prettify(c, blackKingStatus, whiteKingStatus);

                    if (columnIndex == 0) {
                        view.append(prettyPiece);
                        continue;
                    }

                    view.append(", ").append(prettyPiece);
                }

                if (Character.isDigit(c)) {
                    if (columnIndex == 0) {
                        view.append(" ");
                        view.append(",  ".repeat(Character.getNumericValue(c) - 1));
                        continue;
                    }

                    view.append(",  ".repeat(Character.getNumericValue(c)));
                }
            }


            if (rowIndex == 8) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("FEN: ")
                        .append(fen)
                        .append('\n');

                continue;
            }

            if (rowIndex == 6) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("-".repeat(26))
                        .append('+')
                        .append("-".repeat(27))
                        .append('\n');

                continue;
            }

            if (rowIndex == 5) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("Turn: ")
                        .append(turn);

                if (turn < 10) {
                    view.append("\t\t\t\t")
                            .append("\t  |   ")
                            .append("Waiting for ")
                            .append(nextMove)
                            .append('\n');
                } else {
                    view.append("\t\t\t\t  ")
                            .append("|   ")
                            .append("Waiting for ")
                            .append(nextMove)
                            .append('\n');
                }

                continue;
            }

            if (rowIndex == 4) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("Move: ")
                        .append(latestMovement)
                        .append(" ".repeat(12))
                        .append("|   ")
                        .append('\n');

                continue;
            }

            if (rowIndex == 3) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("-".repeat(26))
                        .append("+")
                        .append("-".repeat(27))
                        .append('\n');

                continue;
            }

            if (rowIndex == 2) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("White king: ");

                if (whiteKingStatus.equals("SAFE")) {
                    whiteKingStatus = ANSI_GREEN + whiteKingStatus + "          " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("CHECK")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "         " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("CHECKMATE")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "     " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("STALEMATE")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "     " + ANSI_RESET;
                }

                view.append(whiteKingStatus)
                        .append("|   ")
                        .append("White captures: [");

                for (Piece piece : whiteCaptures) {
                    view.append(pieceToPretty(piece));
                }
                view.append("]").append('\n');
                continue;
            }

            if (rowIndex == 1) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("Black king: ");

                if (blackKingStatus.equals("SAFE")) {
                    blackKingStatus = ANSI_GREEN + blackKingStatus + "          " + ANSI_RESET;
                }
                if (blackKingStatus.equals("CHECK")) {
                    blackKingStatus = ANSI_RED + blackKingStatus + "         " + ANSI_RESET;
                }
                if (blackKingStatus.equals("CHECKMATE")) {
                    blackKingStatus = ANSI_RED + blackKingStatus + "     " + ANSI_RESET;
                }
                if (blackKingStatus.equals("STALEMATE")) {
                    blackKingStatus = ANSI_DARK_RED + blackKingStatus + "     " + ANSI_RESET;
                }

                view.append(blackKingStatus)
                        .append("|   ")
                        .append("Black captures: [");

                for (Piece piece : blackCaptures) {
                    view.append(pieceToPretty(piece));
                }

                view.append("]").append('\n');
                continue;
            }

            view.append("] ").append(rowIndex).append("   | ").append('\n');
        }

        view.append("   A  B  C  D  E  F  G  H      |    --------------------------+---------------------------");
        view.append('\n');

        return view.toString();
    }

    private String pieceToPretty(Piece piece) {
        return switch (piece) {
            case Pawn(Color c) -> c.equals(Color.WHITE) ? "♟" : "♙";
            case Knight(Color c) -> c.equals(Color.WHITE) ? "♞" : "♘";
            case Bishop(Color c) -> c.equals(Color.WHITE) ? "♝" : "♗";
            case Rook(Color c) -> c.equals(Color.WHITE) ? "♜" : "♖";
            case Queen(Color c) -> c.equals(Color.WHITE) ? "♛" : "♕";
            default -> "";
        };
    }

    private static String prettify(char c, String blackKingStatus, String whiteKingStatus) {
        String ANSI_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";

        String prettyPiece = switch (c) {
            case 'P' -> "♟";
            case 'N' -> "♞";
            case 'B' -> "♝";
            case 'R' -> "♜";
            case 'Q' -> "♛";
            case 'K' -> "♚";
            case 'p' -> "♙";
            case 'n' -> "♘";
            case 'b' -> "♗";
            case 'r' -> "♖";
            case 'q' -> "♕";
            case 'k' -> "♔";
            default -> "";
        };

        if ("♔".equals(prettyPiece) && blackKingStatus.equals("CHECK")) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♔".equals(prettyPiece) && (blackKingStatus.equals("CHECKMATE") || blackKingStatus.equals("STALEMATE"))) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♚".equals(prettyPiece) && whiteKingStatus.equals("CHECK")) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♚".equals(prettyPiece) && (whiteKingStatus.equals("CHECKMATE") || whiteKingStatus.equals("STALEMATE"))) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        return prettyPiece;
    }

    /**
     * Retrieves the coordinate of the King piece for the specified color on the given chess board.
     *
     * @param color The {@link Color} enum value (WHITE or BLACK) indicating which King's position to retrieve.
     * @return The {@link Coordinate} object representing the position of the specified King on the board.
     * @throws NullPointerException if either the board or color parameter is null.
     * @see ChessBoard
     * @see Color
     * @see Coordinate
     */
    public Coordinate kingCoordinate(Color color) {
        Objects.requireNonNull(color);

        if (color.equals(Color.WHITE)) {
            return board.currentWhiteKingPosition();
        } else {
            return board.currentBlackKingPosition();
        }
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
     * @param color The {@link Color} enum value (WHITE or BLACK) indicating which pieces to consider as friendly.
     * @return A list of {@link ChessBoard.Field} objects representing the fields that contain friendly pieces.
     * @throws NullPointerException if either {@code chessBoard} or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Color
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> allFriendlyFields(Color color) {
        Objects.requireNonNull(color);

        final Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates)
                .map(board::field)
                .filter(ChessBoard.Field::isPresent)
                .filter(field -> field.pieceOptional().orElseThrow().color().equals(color))
                .toList();
    }

    /**
     * Retrieves all fields on the chess board that contain pieces of the specified color
     * and satisfy the given predicate.
     *
     * <p>This method is an extension of the basic {@link #allFriendlyFields(Color) allFriendlyFields} method.
     * It first finds all friendly fields, then applies a provided predicate to each field before returning the result.</p>
     *
     * <p>This method filters the chess board to find all fields that:</p>
     * <ol>
     *   <li>Contain a piece (are not empty).</li>
     *   <li>Contain a piece of the specified color.</li>
     *   <li>Satisfy the provided predicate.</li>
     * </ol>
     *
     * @param color     The {@link  Color} enum value (WHITE or BLACK) indicating which pieces to consider as friendly.
     * @param predicate A {@link  Predicate<ChessBoard.Field>} used to further filter the fields.
     * @return A list of {@link  ChessBoard.Field} objects representing the fields that meet all the criteria.
     * @throws NullPointerException if any of the parameters {@code chessBoard}, {@code color}, or {@code predicate} is {@code null}.
     * @see ChessBoard
     * @see Color
     * @see ChessBoard.Field
     * @see Predicate
     */
    public List<ChessBoard.Field> allFriendlyFields(Color color, Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(color);
        Objects.requireNonNull(predicate);

        List<ChessBoard.Field> fields = allFriendlyFields(color);

        return fields.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Retrieves all the surrounding fields of a given position on the chess board.
     *
     * <p>This method returns a list of all adjacent fields, including diagonals.
     * It considers all eight directions: up, down, left, right, and all four diagonals.
     * <b>Only fields that exist on the board are included in the result.</b></p>
     *
     * @param pivot The {@link Coordinate} object representing the central position to find surrounding fields for.
     * @return A list of {@link ChessBoard.Field} objects representing all valid surrounding fields.
     * @throws NullPointerException if either {@code chessBoard} or {@code pivot} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> surroundingFields(Coordinate pivot) {
        Objects.requireNonNull(pivot);

        int row = pivot.getRow();
        int column = pivot.columnToInt();

        int[][] directions = {
                {1, 0}, {-1, 0}, {0, -1}, {0, 1},   // up, down, left, right
                {1, -1}, {1, 1}, {-1, -1}, {-1, 1}  // upper-left, upper-right, down-left, down-right
        };

        List<ChessBoard.Field> list = new ArrayList<>();

        for (int[] direction : directions) {
            var possibleCoordinate = Coordinate.of(row + direction[0], column + direction[1]);

            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = board.field(coordinate);
                list.add(field);
            }
        }

        return list;
    }

    /**
     * Retrieves and maps all the surrounding fields of a given position on the chess board.
     *
     * <p>This method is an extension of the basic {@link #surroundingFields(Coordinate) surroundingFields} method.
     * It first finds all surrounding fields, then applies a provided predicate function to each field before returning the result.</p>
     *
     * @param pivot      The {@link Coordinate} object representing the central position to find surrounding fields for.
     * @param predicate  A {@link Predicate<ChessBoard.Field>} that will be applied to each surrounding field.
     * @return A list of {@link ChessBoard.Field} objects representing all valid surrounding fields, after applying the mapping function.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code mapping} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> surroundingFields(Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(predicate);

        var surroundings = surroundingFields(pivot);
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
     * @param pivot The {@code Coordinate} object representing the position of the pawn.
     * @param color The {@code Color} enum value (WHITE or BLACK) indicating the color of the pawn.
     * @return A {@code List<ChessBoard.Field>} representing the fields threatened by the pawn.
     * This list may include fields that are empty.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see Color
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> coordinatesThreatenedByPawn(Coordinate pivot, Color color) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.of(pivot.getRow() + 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.of(pivot.getRow() + 1, pivot.columnToInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.of(pivot.getRow() - 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.of(pivot.getRow() - 1, pivot.columnToInt() + 1));
        }

        List<ChessBoard.Field> fields = new ArrayList<>();
        for (var possibleCoordinate : possibleCoordinates) {
            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = board.field(coordinate);
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
     * @param pivot The list of{@link Coordinate} object representing the position being threatened.
     * @param color The list of{@link Color} enum value (WHITE or BLACK) indicating the color of the threatening pawns.
     * @return A list of{@link ChessBoard.Field} objects representing the fields containing pawns that threaten the pivot coordinate.
     * @throws NullPointerException if any of {@code chessBoard}, {@code pivot}, or {@code color} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see Color
     * @see ChessBoard.Field
     * @see Pawn
     */
    public List<ChessBoard.Field> pawnsThreateningCoordinate(Coordinate pivot, Color color) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final List<StatusPair<Coordinate>> possibleCoordinates = new ArrayList<>(2);

        if (Color.WHITE.equals(color)) {
            possibleCoordinates.add(Coordinate.of(pivot.getRow() - 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.of(pivot.getRow() - 1, pivot.columnToInt() + 1));
        } else {
            possibleCoordinates.add(Coordinate.of(pivot.getRow() + 1, pivot.columnToInt() - 1));
            possibleCoordinates.add(Coordinate.of(pivot.getRow() + 1, pivot.columnToInt() + 1));
        }

        List<ChessBoard.Field> fields = new ArrayList<>();
        for (var possibleCoordinate : possibleCoordinates) {
            if (!possibleCoordinate.status()) {
                continue;
            }

            Coordinate coordinate = possibleCoordinate.orElseThrow();
            var field = board.field(coordinate);

            if (field.isEmpty()) {
                continue;
            }

            Piece piece = field.pieceOptional().orElseThrow();
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
     * @param pivot The {@link Coordinate} object representing the starting position of the knight.
     * @return A list of{@link ChessBoard.Field} objects representing all valid positions a knight can attack from the given pivot.
     * @throws NullPointerException if either {@code chessBoard} or {@code pivot} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> knightAttackPositions(Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(predicate);

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
            var possibleCoordinate = Coordinate.of(row + move[0], col + move[1]);

            if (possibleCoordinate.status()) {
                Coordinate coordinate = possibleCoordinate.orElseThrow();
                ChessBoard.Field field = board.field(coordinate);

                if (predicate.test(field)) {
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
     * @param color
     * @return A list of {@link ChessBoard.Field} objects representing all fields involved in the castling move, including the king's path.
     * @throws NullPointerException  if any of {@code chessBoard}, {@code presentKing}, or {@code futureKingPosition} is {@code null}.
     * @throws IllegalStateException if an invalid coordinate is encountered during calculation.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> castlingFields(AlgebraicNotation.Castle castle, Color color) {
        Objects.requireNonNull(castle);
        Objects.requireNonNull(color);

        return switch (color) {
            case WHITE -> {
                if (castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING)) {
                    yield  List.of(board.field(Coordinate.e1), board.field(Coordinate.f1), board.field(Coordinate.g1));
                }
                yield  List.of(board.field(Coordinate.e1), board.field(Coordinate.d1), board.field(Coordinate.c1));
            }
            case BLACK -> {
                if (castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING)) {
                    yield  List.of(board.field(Coordinate.e8), board.field(Coordinate.f8), board.field(Coordinate.g8));
                }
                yield  List.of(board.field(Coordinate.e8), board.field(Coordinate.d8), board.field(Coordinate.c8));
            }
        };
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
     * @param coordinate The {@link Coordinate} of the piece whose forward field is to be determined.
     * @return An {@link Optional} containing the {@link ChessBoard.Field} directly in front of the specified piece,
     * or an empty {@link Optional} if the forward position is outside the board or invalid.
     * @throws NullPointerException if either the chessBoard or coordinate is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Optional
     */
    public Optional<ChessBoard.Field> forwardField(Coordinate coordinate) {
        Objects.requireNonNull(coordinate);

        ChessBoard.Field field = board.field(coordinate);
        Piece piece = field.pieceOptional().orElseThrow();

        int direction = piece.color().equals(Color.WHITE) ? 1 : -1;
        final var possibleForwardCoordinate = Coordinate.of(coordinate.getRow() + direction, coordinate.columnToInt());

        if (possibleForwardCoordinate.status()) {
            Coordinate forward = possibleForwardCoordinate.orElseThrow();
            ChessBoard.Field forwardField = board.field(forward);

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
    public Optional<ChessBoard.Field> forwardField(Coordinate coordinate, Predicate<ChessBoard.Field> predicate) {
        Objects.requireNonNull(coordinate);
        Objects.requireNonNull(predicate);

        Optional<ChessBoard.Field> forward = forwardField(coordinate);

        if (forward.isPresent() && predicate.test(forward.get())) {
            return forward;
        }

        return Optional.empty();
    }

    /**
     * Retrieves the fields in specified directions from a given pivot coordinate that are occupied by any piece.
     *
     * <p>This method scans the board in each of the provided directions starting from the pivot coordinate,
     * and collects the first field that contains a piece. The search in each direction stops as soon as an occupied
     * field is found.</p>
     *
     * <p>The result is a list of fields that:
     * <ol>
     *   <li>Are in one of the specified directions from the pivot.</li>
     *   <li>Contain a piece (are occupied).</li>
     * </ol>
     * </p>
     *
     * @param directions A list of {@link Direction} objects representing the directions to scan from the pivot.
     * @param pivot      The {@link Coordinate} object representing the starting position.
     * @return A list of {@link ChessBoard.Field} objects representing the first occupied fields found in each direction.
     * @throws NullPointerException if any of the parameters {@code directions}, {@code pivot}, or {@code chessBoard} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Direction
     */
    public List<ChessBoard.Field> occupiedFieldsInDirections(List<Direction> directions, Coordinate pivot) {
        List<ChessBoard.Field> list = new ArrayList<>();

        for (Direction direction : directions) {
            Optional<ChessBoard.Field> possibleField = occupiedFieldInDirection(direction, pivot);

            if (possibleField.isPresent()) {
                ChessBoard.Field field = possibleField.get();
                list.add(field);
            }
        }

        return list;
    }

    /**
     * Retrieves the fields in specified directions from a given pivot coordinate that are occupied and satisfy a predicate.
     *
     * <p>This method is an extension of the basic {@link #occupiedFieldsInDirections(List, Coordinate)} method.
     * It scans the board in each of the provided directions starting from the pivot coordinate and collects the first
     * occupied field that satisfies the provided predicate. The search in each direction stops as soon as an occupied
     * field is found.</p>
     *
     * <p>The result is a list of fields that:
     * <ol>
     *   <li>Are in one of the specified directions from the pivot.</li>
     *   <li>Contain a piece (are occupied).</li>
     *   <li>Satisfy the provided predicate.</li>
     * </ol>
     * </p>
     *
     * @param directions        A list of {@link Direction} objects representing the directions to scan from the pivot.
     * @param pivot             The {@link Coordinate} object representing the starting position.
     * @param occupiedPredicate A {@link Predicate} used to further filter the occupied fields.
     * @return A list of {@link ChessBoard.Field} objects representing the first occupied fields that satisfy the predicate in each direction.
     * @throws NullPointerException if any of the parameters {@code directions}, {@code pivot}, {@code occupiedPredicate}, or {@code chessBoard} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Direction
     * @see Predicate
     */
    public List<ChessBoard.Field> occupiedFieldsInDirections(List<Direction> directions,
                                                             Coordinate pivot,
                                                             Predicate<ChessBoard.Field> occupiedPredicate) {
        List<ChessBoard.Field> list = new ArrayList<>();
        for (Direction direction : directions) {
            Optional<ChessBoard.Field> optionalField = occupiedFieldInDirection(direction, pivot, occupiedPredicate);

            if (optionalField.isPresent()) {
                ChessBoard.Field field = optionalField.get();
                list.add(field);
            }
        }
        return list;
    }


    /**
     * Retrieves the fields in specified directions from a given pivot coordinate that are occupied, with advanced filtering.
     *
     * <p>This method scans the board in each of the provided directions starting from the pivot coordinate and applies
     * multiple predicates to determine which fields to skip, where to stop, and which occupied fields to include in the result.
     * The search in each direction proceeds according to the following steps:</p>
     * <ol>
     *   <li>Skip fields that match the {@code skipPredicate}.</li>
     *   <li>Stop the search in a direction when a field matches the {@code stopPredicate}.</li>
     *   <li>If an occupied field is found that matches the {@code occupiedPredicate}, it is included in the result.</li>
     * </ol>
     *
     * <p>The result is a list of fields that:
     * <ul>
     *   <li>Are in one of the specified directions from the pivot.</li>
     *   <li>Are occupied by a piece.</li>
     *   <li>Satisfy all the provided predicates.</li>
     * </ul>
     * </p>
     *
     * @param directions        A list of {@link Direction} objects representing the directions to scan from the pivot.
     * @param pivot             The {@link Coordinate} object representing the starting position.
     * @param skipPredicate     A {@link Predicate} used to determine which fields to skip during the search.
     * @param stopPredicate     A {@link Predicate} used to determine when to stop the search in a particular direction.
     * @param occupiedPredicate A {@link Predicate} used to further filter the occupied fields.
     * @return A list of {@link ChessBoard.Field} objects representing the first occupied fields that satisfy all the predicates in each direction.
     * @throws NullPointerException if any of the parameters {@code directions}, {@code pivot}, {@code skipPredicate}, {@code stopPredicate}, {@code occupiedPredicate}, or {@code chessBoard} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see ChessBoard.Field
     * @see Direction
     * @see Predicate
     */
    public List<ChessBoard.Field> occupiedFieldsInDirections(List<Direction> directions, Coordinate pivot,
                                                             Predicate<Coordinate> skipPredicate,
                                                             Predicate<Coordinate> stopPredicate,
                                                             Predicate<ChessBoard.Field> occupiedPredicate) {
        return directions.stream()
                .map(direction -> occupiedFieldInDirection(direction, pivot, skipPredicate, stopPredicate, occupiedPredicate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Retrieves a list of fields in specified directions from a given pivot coordinate on the chessboard.
     *
     * <p>This method calculates the fields in multiple specified directions starting from the given
     * pivot coordinate. The directions are defined using the {@link Direction} enum, and the fields
     * are collected in the order they are encountered along each direction.</p>
     *
     * <p>If a field lies outside the chessboard's boundaries, that direction's search stops, and no
     * more fields are collected from that direction.</p>
     *
     * @param directions A list of {@link Direction} values representing the directions in which to search for fields.
     * @param pivot      The {@link Coordinate} object representing the starting position on the chessboard.
     * @return A list of {@link ChessBoard.Field} objects representing all the fields found in the specified directions.
     *         The fields are returned in the order they are encountered along each direction.
     * @throws NullPointerException if either {@code directions} or {@code pivot} is {@code null}.
     * @see ChessBoard
     * @see Coordinate
     * @see Direction
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> fieldsInDirections(List<Direction> directions, Coordinate pivot) {
        return directions.stream()
                .map(direction -> fieldInDirection(direction, pivot))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }


    /**
     * Retrieves a list of fields on the chessboard along the straight path between two coordinates.
     *
     * <p>This method calculates the fields along a straight path from a starting {@link Coordinate} to an ending
     * {@link Coordinate}, using the direction determined by these two coordinates. The path is traversed one step at a time,
     * collecting the fields encountered. The method can include or exclude the starting and ending coordinates based on the
     * {@code inclusive} parameter.</p>
     *
     * <p>If the path between the start and end coordinates is invalid (i.e., not a straight line or outside of the
     * chessboard's boundaries), {@link IllegalArgumentException} is thrown.</p>
     *
     * @param start     The {@link Coordinate} object representing the starting position on the chessboard.
     * @param end       The {@link Coordinate} object representing the ending position on the chessboard.
     * @param inclusive A boolean value indicating whether to include the starting and ending coordinates in the
     *                  list of fields. If {@code true}, both the start and end fields are included.
     * @return A list of {@link ChessBoard.Field} objects representing the fields along the path from {@code start} to
     *         {@code end}. The fields are returned in the order they are encountered.
     * @throws NullPointerException if either {@code start} or {@code end} is {@code null}.
     * @throws IllegalArgumentException if {@code start} and {@code end} do not define a valid straight path.
     * @see ChessBoard
     * @see Coordinate
     * @see Direction
     * @see ChessBoard.Field
     */
    public List<ChessBoard.Field> fieldsInPath(Coordinate start, Coordinate end, boolean inclusive) {
        Direction direction = Direction.ofPath(start, end);
        List<ChessBoard.Field> fields = new ArrayList<>();

        if (inclusive) {
            fields.add(board.field(start));
        }

        Coordinate current = start;
        while (true) {
            StatusPair<Coordinate> next = direction.apply(current);
            if (!next.status()) {
                break;
            }

            current = next.orElseThrow();
            if (current.equals(end) && inclusive) {
                fields.add(board.field(current));
                break;
            }

            if (current.equals(end)) {
                break;
            }

            fields.add(board.field(current));
        }

        return fields;
    }

    /**
     * Finds the first occupied field in a given direction from a specified pivot coordinate on the chessboard.
     *
     *
     * @param direction The direction in which to search for the occupied field.
     * @param pivot The starting point on the chessboard from which to search.
     * @return An `Optional` containing the first `ChessBoard.Field` that is occupied in the specified direction, or `Optional.empty()` if no field is found.
     */
    public Optional<ChessBoard.Field> occupiedFieldInDirection(Direction direction, Coordinate pivot) {
        return occupiedFieldInDirection(direction, pivot, coordinate -> false, coordinate -> false, field -> true);
    }

    /**
     * Finds the first occupied field in a given direction from a specified pivot coordinate on the chessboard,
     * using a custom predicate to determine what constitutes an occupied field.
     *
     *
     * @param direction The direction in which to search for the occupied field.
     * @param pivot The starting point on the chessboard from which to search.
     * @param predicate A `Predicate` to test whether a `ChessBoard.Field` is considered occupied.
     * @return An `Optional` containing the first `ChessBoard.Field` that matches the predicate in the specified direction, or `Optional.empty()` if no field is found.
     */
    public Optional<ChessBoard.Field> occupiedFieldInDirection(Direction direction, Coordinate pivot, Predicate<ChessBoard.Field> predicate) {
        return occupiedFieldInDirection(direction, pivot, coordinate -> false, coordinate -> false, predicate);
    }

    /**
     * Finds the first occupied field in a given direction from a specified pivot coordinate on the chessboard,
     * using custom predicates to control the search process.
     *
     * @param direction The direction in which to search for the occupied field.
     * @param pivot The starting point on the chessboard from which to search.
     * @param skipPredicate A `Predicate` to determine whether to skip a coordinate.
     * @param stopPredicate A `Predicate` to determine whether to stop the search at a coordinate.
     * @param occupiedPredicate A `Predicate` to test whether a `ChessBoard.Field` is considered occupied.
     * @return An `Optional` containing the first `ChessBoard.Field` that matches the `occupiedPredicate` in the specified direction, or `Optional.empty()` if no field is found.
     */
    public Optional<ChessBoard.Field> occupiedFieldInDirection(Direction direction, Coordinate pivot,
                                                               Predicate<Coordinate> skipPredicate,
                                                               Predicate<Coordinate> stopPredicate,
                                                               Predicate<ChessBoard.Field> occupiedPredicate) {
        for (Coordinate coord : new CoordinateIterable(direction, pivot)) {
            if (skipPredicate.test(coord)) {
                continue;
            }

            if (stopPredicate.test(coord)) {
                break;
            }

            ChessBoard.Field field = board.field(coord);
            if (field.isPresent() && occupiedPredicate.test(field)) {
                return Optional.of(field);
            }

        }
        return Optional.empty();
    }

    /**
     * Finds the field at the first coordinate in a given direction from a specified pivot coordinate on the chessboard.
     *
     * @param direction The direction in which to search for the field.
     * @param pivot The starting point on the chessboard from which to search.
     * @return An `Optional` containing the `ChessBoard.Field` at the first coordinate in the specified direction, or `Optional.empty()` if no field is found.
     */
    public Optional<ChessBoard.Field> fieldInDirection(Direction direction, Coordinate pivot) {
        Iterator<Coordinate> iterator = new CoordinateIterable(direction, pivot).iterator();

        if (iterator.hasNext()) {
            return Optional.of(board.field(iterator.next()));
        }

        return Optional.empty();
    }

    public List<Coordinate> fieldsForPawnMovement(Coordinate pivot, Color color) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final int column = pivot.columnToInt();
        final int row = pivot.getRow();

        if (color.equals(Color.WHITE)) {
            final var top = Coordinate.of(row + 1, column);
            final var topLeft = Coordinate.of(row + 1, column - 1);
            final var topRight = Coordinate.of(row + 1, column + 1);
            final var topTop = Coordinate.of(row + 2, column);

            return Stream.of(top, topLeft, topRight, topTop)
                    .filter(StatusPair::status)
                    .map(StatusPair::orElseThrow)
                    .toList();
        }

        final var bottom = Coordinate.of(row - 1, column);
        final var bottomLeft = Coordinate.of(row - 1, column - 1);
        final var bottomRight = Coordinate.of(row - 1, column + 1);
        final var bottomBottom = Coordinate.of(row - 2, column);

        return Stream.of(bottom, bottomLeft, bottomRight, bottomBottom)
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .toList();
    }

    /**
     * An iterable collection of {@link Coordinate} objects that can be traversed
     * in a specific {@link Direction} starting from a given {@link Coordinate}.
     *
     * <p>This class implements the {@link Iterable} interface, allowing instances
     * to be used in enhanced for-loops and other iterable contexts.</p>
     *
     * <p>The iteration is controlled by the provided {@link Direction} which
     * determines the next {@link Coordinate} in the sequence. The iteration
     * continues until the {@link Direction} returns a status indicating
     * that there are no more coordinates to traverse.</p>
     */
    private record CoordinateIterable(Direction direction, Coordinate start) implements Iterable<Coordinate> {

        @Override
        @NonNull
        public Iterator<Coordinate> iterator() {
            return new Iterator<>() {
                private Coordinate current = start;

                @Override
                public boolean hasNext() {
                    return direction.apply(current).status();
                }

                @Override
                public Coordinate next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    var result = direction.apply(current).orElseThrow();
                    current = result;
                    return result;
                }
            };
        }
    }
}
