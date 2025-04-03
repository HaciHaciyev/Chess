package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;

import java.util.*;
import java.util.function.Predicate;

public record ChessBoardNavigator(ChessBoard board) {

    private static final int[][] SURROUNDING_DIRECTIONS = new int[][]{
            {1, 0}, {-1, 0}, {0, -1}, {0, 1},   // up, down, left, right
            {1, -1}, {1, 1}, {-1, -1}, {-1, 1}  // upper-left, upper-right, down-left, down-right
    };

    private static final int[][] KNIGHT_ATTACKS = new int[][]{
            {1, -2}, {2, -1},   // top-left
            {2, 1}, {1, 2},     // top-right
            {-1, 2}, {-2, 1},   // bottom-right
            {-2, -1}, {-1, -2}  // bottom-left
    };

    private static final Coordinate[] coordinates = Coordinate.values();

    public Coordinate kingCoordinate(Color color) {
        if (color == Color.WHITE) {
            return board.currentWhiteKingPosition();
        }

        return board.currentBlackKingPosition();
    }

    public List<Coordinate> allFriendlyFieldsExceptKing(Color color, Coordinate kingCoordinate) {
        List<Coordinate> friendlyFields = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            Piece piece = board.piece(coordinate);
            if (piece != null && piece.color() == color && coordinate != kingCoordinate) friendlyFields.add(coordinate);
        }

        return friendlyFields;
    }

    public List<Coordinate> surroundingFields(Coordinate pivot) {
        int row = pivot.row();
        int column = pivot.column();

        List<Coordinate> result = new ArrayList<>(8);

        for (int[] direction : SURROUNDING_DIRECTIONS) {
            Coordinate possibleCoordinate = Coordinate.of(row + direction[0], column + direction[1]);
            if (possibleCoordinate != null) result.add(possibleCoordinate);
        }

        return result;
    }

    public boolean findKingOpposition(Coordinate pivot, Color oppositeKingColor) {
        int row = pivot.row();
        int column = pivot.column();

        for (int[] direction : SURROUNDING_DIRECTIONS) {
            Coordinate possibleCoordinate = Coordinate.of(row + direction[0], column + direction[1]);
            if (possibleCoordinate != null) {
                Piece piece = board.piece(possibleCoordinate);
                if (piece != null && piece.color() == oppositeKingColor && piece instanceof King) return true;
            }
        }

        return false;
    }

    public List<Coordinate> pawnsThreateningTheCoordinateOf(Coordinate pivot, Color colorOfRequiredPawns) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(colorOfRequiredPawns);

        final List<Coordinate> possibleCoordinates = new ArrayList<>(2);
        if (colorOfRequiredPawns == Color.WHITE) {
            Coordinate first = Coordinate.of(pivot.row() - 1, pivot.column() - 1);
            if (isPawnExists(colorOfRequiredPawns, first)) {
                possibleCoordinates.add(first);
            }

            Coordinate second = Coordinate.of(pivot.row() - 1, pivot.column() + 1);
            if (isPawnExists(colorOfRequiredPawns, second)) {
                possibleCoordinates.add(second);
            }

            return possibleCoordinates;
        }

        Coordinate first = Coordinate.of(pivot.row() + 1, pivot.column() - 1);
        if (isPawnExists(colorOfRequiredPawns, first)) {
            possibleCoordinates.add(first);
        }

        Coordinate second = Coordinate.of(pivot.row() + 1, pivot.column() + 1);
        if (isPawnExists(colorOfRequiredPawns, second)) {
            possibleCoordinates.add(second);
        }
        return possibleCoordinates;
    }

    private boolean isPawnExists(Color colorOfRequiredPawns, Coordinate coordinate) {
        return coordinate != null && board.piece(coordinate) instanceof Pawn pawn && pawn.color() == colorOfRequiredPawns;
    }

    public List<Coordinate> knightAttackPositions(Coordinate pivot) {
        Objects.requireNonNull(pivot);

        int row = pivot.row();
        int col = pivot.column();

        List<Coordinate> fields = new ArrayList<>();

        for (int[] move : KNIGHT_ATTACKS) {
            Coordinate possibleCoordinate = Coordinate.of(row + move[0], col + move[1]);
            if (possibleCoordinate != null) fields.add(possibleCoordinate);
        }

        return fields;
    }

    public List<Coordinate> knightAttackPositions(Coordinate pivot, Predicate<Piece> predicate) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(predicate);

        int row = pivot.row();
        int col = pivot.column();

        List<Coordinate> fields = new ArrayList<>();

        for (int[] move : KNIGHT_ATTACKS) {
            Coordinate possibleCoordinate = Coordinate.of(row + move[0], col + move[1]);

            if (possibleCoordinate != null) {
                Piece field = board.piece(possibleCoordinate);
                if (predicate.test(field)) fields.add(possibleCoordinate);
            }
        }

        return fields;
    }

    public List<Coordinate> castlingFields(AlgebraicNotation.Castle castle, Color color) {
        Objects.requireNonNull(castle);
        Objects.requireNonNull(color);

        return switch (color) {
            case WHITE -> {
                if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                    yield List.of(Coordinate.e1, Coordinate.f1, Coordinate.g1);
                }
                yield List.of(Coordinate.e1, Coordinate.d1, Coordinate.c1);
            }
            case BLACK -> {
                if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                    yield List.of(Coordinate.e8, Coordinate.f8, Coordinate.g8);
                }
                yield List.of(Coordinate.e8, Coordinate.d8, Coordinate.c8);
            }
        };
    }

    public List<Coordinate> occupiedFieldsInDirections(List<Direction> directions, Coordinate pivot) {
        List<Coordinate> list = new ArrayList<>();
        for (Direction direction : directions) {
            Coordinate possibleField = occupiedFieldInDirection(direction, pivot);
            if (possibleField != null) list.add(possibleField);
        }
        return list;
    }

    public List<Coordinate> occupiedFieldsInDirections(List<Direction> directions, Coordinate pivot, int requiredEnemiesCount) {
        List<Coordinate> list = new ArrayList<>();
        for (Direction direction : directions) {
            Coordinate possibleField = occupiedFieldInDirection(direction, pivot);
            if (possibleField != null) {
                list.add(possibleField);
                if (requiredEnemiesCount == list.size()) return list;
            }
        }
        return list;
    }

    public List<Coordinate> occupiedFieldsInDirections(List<Direction> directions,
                                                       Coordinate pivot,
                                                       Predicate<Coordinate> occupiedPredicate) {
        List<Coordinate> list = new ArrayList<>();
        for (Direction direction : directions) {
            Coordinate optionalField = occupiedFieldInDirection(direction, pivot, occupiedPredicate);
            if (optionalField != null && board.piece(optionalField) != null) list.add(optionalField);
        }
        return list;
    }

    public List<Coordinate> occupiedFieldsInDirections(List<Direction> directions, Coordinate pivot,
                                                       Predicate<Coordinate> skipPredicate,
                                                       Predicate<Coordinate> stopPredicate) {
        List<Coordinate> list = new ArrayList<>();
        for (Direction direction : directions) {
            Coordinate coordinate = occupiedFieldInDirection(direction, pivot, skipPredicate, stopPredicate, x -> true);
            if (coordinate != null && board.piece(coordinate) != null) list.add(coordinate);
        }
        return list;
    }

    public List<Coordinate> fieldsInDirections(List<Direction> directions, Coordinate pivot) {
        List<Coordinate> list = new ArrayList<>();
        for (Direction direction : directions) list.addAll(fieldsInDirection(direction, pivot));
        return list;
    }

    public List<Coordinate> fieldsInPath(Coordinate start, Coordinate end, boolean inclusive) {
        Direction direction = Direction.ofPath(start, end);
        List<Coordinate> fields = new ArrayList<>();

        if (inclusive) {
            fields.add(start);
        }

        Coordinate current = start;
        while (true) {
            Coordinate next = direction.apply(current);
            if (next == null) {
                break;
            }

            current = next;
            if (current.equals(end) && inclusive) {
                fields.add(current);
                break;
            }

            if (current.equals(end)) {
                break;
            }

            fields.add(current);
        }

        return fields;
    }

    public Coordinate occupiedFieldInDirection(Direction direction, Coordinate pivot) {
        return occupiedFieldInDirection(direction, pivot, coordinate -> false, coordinate -> false, field -> true);
    }

    public Coordinate occupiedFieldInDirection(Direction direction, Coordinate pivot, Predicate<Coordinate> predicate) {
        return occupiedFieldInDirection(direction, pivot, coordinate -> false, coordinate -> false, predicate);
    }

    public Coordinate occupiedFieldInDirection(Direction direction, Coordinate pivot,
                                               Predicate<Coordinate> skipPredicate,
                                               Predicate<Coordinate> stopPredicate,
                                               Predicate<Coordinate> occupiedPredicate) {
        for (Coordinate field : new CoordinateIterable(direction, pivot)) {
            if (skipPredicate.test(field)) continue;

            if (stopPredicate.test(field)) break;

            Piece piece = board.piece(field);
            if (piece != null && occupiedPredicate.test(field)) return field;
        }
        return null;
    }

    private List<Coordinate> fieldsInDirection(Direction direction, Coordinate pivot) {
        List<Coordinate> listOfFields = new ArrayList<>();

        for (Coordinate coordinate : new CoordinateIterable(direction, pivot)) {
            Piece piece = board.piece(coordinate);
            if (piece != null) {
                listOfFields.add(coordinate);
                return listOfFields;
            }

            listOfFields.add(coordinate);
        }

        return listOfFields;
    }

    public List<Coordinate> fieldsForPawnMovement(Coordinate pivot, Color color) {
        Objects.requireNonNull(pivot);
        Objects.requireNonNull(color);

        final int column = pivot.column();
        final int row = pivot.row();

        List<Coordinate> result = new ArrayList<>();

        if (color == Color.WHITE) {
            Coordinate top = Coordinate.of(row + 1, column);
            Coordinate topLeft = Coordinate.of(row + 1, column - 1);
            Coordinate topRight = Coordinate.of(row + 1, column + 1);
            Coordinate topTop = null;
            if (row == 2) {
                topTop = Coordinate.of(row + 2, column);
            }

            if (top != null) {
                result.add(top);
            }
            if (topLeft != null) {
                result.add(topLeft);
            }
            if (topRight != null) {
                result.add(topRight);
            }
            if (topTop != null) {
                result.add(topTop);
            }
            return result;
        }

        Coordinate bottom = Coordinate.of(row - 1, column);
        Coordinate bottomLeft = Coordinate.of(row - 1, column - 1);
        Coordinate bottomRight = Coordinate.of(row - 1, column + 1);
        Coordinate bottomBottom = null;
        if (row == 7) {
            bottomBottom = Coordinate.of(row - 2, column);
        }

        if (bottom != null) {
            result.add(bottom);
        }
        if (bottomLeft != null) {
            result.add(bottomLeft);
        }
        if (bottomRight != null) {
            result.add(bottomRight);
        }
        if (bottomBottom != null) {
            result.add(bottomBottom);
        }
        return result;
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
        public Iterator<Coordinate> iterator() {
            return new Iterator<>() {
                private Coordinate current = start;

                @Override
                public boolean hasNext() {
                    return direction.apply(current) != null;
                }

                @Override
                public Coordinate next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    var result = direction.apply(current);
                    current = result;
                    return result;
                }
            };
        }
    }

    public String prettyToString() {
        String fen = board.toString();
        int space = fen.indexOf(" ");

        String boardRepresentation = fen.substring(0, space);
        int turn = board.countOfHalfMoves() + 1;
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

                    view.append(". ").append(prettyPiece);
                }

                if (Character.isDigit(c)) {
                    if (columnIndex == 0) {
                        view.append(" ");
                        view.append(".  ".repeat(Character.getNumericValue(c) - 1));
                        continue;
                    }

                    view.append(".  ".repeat(Character.getNumericValue(c)));
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

            if (rowIndex == 7) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("PGN: ")
                        .append(board.pgn())
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
            case Pawn pawn -> pawn.color() == Color.WHITE ? "♟" : "♙";
            case Knight knight -> knight.color() == Color.WHITE ? "♞" : "♘";
            case Bishop bishop -> bishop.color() == Color.WHITE ? "♝" : "♗";
            case Rook rook -> rook.color() == Color.WHITE ? "♜" : "♖";
            case Queen queen -> queen.color() == Color.WHITE ? "♛" : "♕";
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
}
