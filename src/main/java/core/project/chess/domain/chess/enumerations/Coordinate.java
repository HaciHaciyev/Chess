package core.project.chess.domain.chess.enumerations;

import jakarta.annotation.Nullable;

public enum Coordinate {

    a8(1, 8), b8(2, 8), c8(3, 8), d8(4, 8), e8(5, 8), f8(6, 8), g8(7, 8), h8(8, 8),
    a7(1, 7), b7(2, 7), c7(3, 7), d7(4, 7), e7(5, 7), f7(6, 7), g7(7, 7), h7(8, 7),
    a6(1, 6), b6(2, 6), c6(3, 6), d6(4, 6), e6(5, 6), f6(6, 6), g6(7, 6), h6(8, 6),
    a5(1, 5), b5(2, 5), c5(3, 5), d5(4, 5), e5(5, 5), f5(6, 5), g5(7, 5), h5(8, 5),
    a4(1, 4), b4(2, 4), c4(3, 4), d4(4, 4), e4(5, 4), f4(6, 4), g4(7, 4), h4(8, 4),
    a3(1, 3), b3(2, 3), c3(3, 3), d3(4, 3), e3(5, 3), f3(6, 3), g3(7, 3), h3(8, 3),
    a2(1, 2), b2(2, 2), c2(3, 2), d2(4, 2), e2(5, 2), f2(6, 2), g2(7, 2), h2(8, 2),
    a1(1, 1), b1(2, 1), c1(3, 1), d1(4, 1), e1(5, 1), f1(6, 1), g1(7, 1), h1(8, 1);

    private final int column;

    private final int row;

    private static final Coordinate[][] COORDINATE_CACHE = new Coordinate[8][8];

    public static final Coordinate WHITE_KING_START = Coordinate.e1;
    public static final Coordinate WHITE_KING_SHORT_CASTLE = Coordinate.g1;
    public static final Coordinate WHITE_KING_LONG_CASTLE = Coordinate.c1;
    public static final Coordinate WHITE_ROOK_SHORT_CASTLE_START = Coordinate.h1;
    public static final Coordinate WHITE_ROOK_SHORT_CASTLE_END = Coordinate.f1;
    public static final Coordinate WHITE_ROOK_LONG_CASTLE_START = Coordinate.a1;
    public static final Coordinate WHITE_ROOK_LONG_CASTLE_END = Coordinate.d1;

    public static final Coordinate BLACK_KING_START = Coordinate.e8;
    public static final Coordinate BLACK_KING_SHORT_CASTLE = Coordinate.g8;
    public static final Coordinate BLACK_KING_LONG_CASTLE = Coordinate.c8;
    public static final Coordinate BLACK_ROOK_SHORT_CASTLE_START = Coordinate.h8;
    public static final Coordinate BLACK_ROOK_SHORT_CASTLE_END = Coordinate.f8;
    public static final Coordinate BLACK_ROOK_LONG_CASTLE_START = Coordinate.a8;
    public static final Coordinate BLACK_ROOK_LONG_CASTLE_END = Coordinate.d8;

    static {
        for (Coordinate c : Coordinate.values()) {
            int row = c.row() - 1;
            int col = c.column() - 1;
            COORDINATE_CACHE[row][col] = c;
        }
    }

    Coordinate(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int row() {
        return row;
    }

    public int column() {
        return column;
    }

    public char columnChar() {
        return intToColumn(column);
    }

    /**
     * Finds the Coordinate object corresponding to the given row and column.
     *
     * @param row    the row number (1-8)
     * @param column the column number (1-8)
     * @return a Coordinate containing the Coordinate object if the row and column are valid,
     * or a null with a false status if the row or column is out of bounds
     */
    public static @Nullable Coordinate of(int row, int column) {
        if (row > 8 || column > 8 || row < 1 || column < 1) {
            return null;
        }

        return COORDINATE_CACHE[row - 1][column - 1];
    }

    /**
     * Converts a column letter (a-h) to the corresponding column number (1-8).
     *
     * @param c the column letter (a-h)
     * @return the corresponding column number (1-8)
     * @throws IllegalStateException if the character is not between 'a' and 'h'
     */
    public static int columnToInt(char c) {
        if (c >= 'a' && c <= 'h') {
            return c - 'a' + 1;
        }
        throw new IllegalStateException("Unexpected value: " + c);
    }

    /**
     * Converts a column number (1-8) to the corresponding column letter (a-h).
     *
     * @param columnNumber the column number (1-8)
     * @return the corresponding column letter (a-h)
     * @throws IllegalStateException if the columnNumber is not between 1 and 8 (inclusive)
     */
    public static char intToColumn(int columnNumber) {
        if (columnNumber >= 1 && columnNumber <= 8) {
            return (char) ('a' + columnNumber - 1);
        }
        throw new IllegalStateException("Unexpected value: " + columnNumber);
    }

    public enum Column {
        A(1), B(2), C(3), D(4), E(5), F(6), G(7), H(8);

        final int column;

        Column(int i) {
            this.column = i;
        }

        public int getColumn() {
            return column;
        }

        public static Column of(final int column) {
            return switch (column) {
                case 1 -> Column.A;
                case 2 -> Column.B;
                case 3 -> Column.C;
                case 4 -> Column.D;
                case 5 -> Column.E;
                case 6 -> Column.F;
                case 7 -> Column.G;
                case 8 -> Column.H;
                default -> throw new IllegalArgumentException("Invalid column number: " + column);
            };
        }
    }
}
