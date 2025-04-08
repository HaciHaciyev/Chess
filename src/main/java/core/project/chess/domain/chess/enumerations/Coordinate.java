package core.project.chess.domain.chess.enumerations;

import jakarta.annotation.Nullable;

public enum Coordinate {
    a8(1, 8, new byte[]{'a', '8'}),
    b8(2, 8, new byte[]{'b', '8'}),
    c8(3, 8, new byte[]{'c', '8'}),
    d8(4, 8, new byte[]{'d', '8'}),
    e8(5, 8, new byte[]{'e', '8'}),
    f8(6, 8, new byte[]{'f', '8'}),
    g8(7, 8, new byte[]{'g', '8'}),
    h8(8, 8, new byte[]{'h', '8'}),

    a7(1, 7, new byte[]{'a', '7'}),
    b7(2, 7, new byte[]{'b', '7'}),
    c7(3, 7, new byte[]{'c', '7'}),
    d7(4, 7, new byte[]{'d', '7'}),
    e7(5, 7, new byte[]{'e', '7'}),
    f7(6, 7, new byte[]{'f', '7'}),
    g7(7, 7, new byte[]{'g', '7'}),
    h7(8, 7, new byte[]{'h', '7'}),

    a6(1, 6, new byte[]{'a', '6'}),
    b6(2, 6, new byte[]{'b', '6'}),
    c6(3, 6, new byte[]{'c', '6'}),
    d6(4, 6, new byte[]{'d', '6'}),
    e6(5, 6, new byte[]{'e', '6'}),
    f6(6, 6, new byte[]{'f', '6'}),
    g6(7, 6, new byte[]{'g', '6'}),
    h6(8, 6, new byte[]{'h', '6'}),

    a5(1, 5, new byte[]{'a', '5'}),
    b5(2, 5, new byte[]{'b', '5'}),
    c5(3, 5, new byte[]{'c', '5'}),
    d5(4, 5, new byte[]{'d', '5'}),
    e5(5, 5, new byte[]{'e', '5'}),
    f5(6, 5, new byte[]{'f', '5'}),
    g5(7, 5, new byte[]{'g', '5'}),
    h5(8, 5, new byte[]{'h', '5'}),

    a4(1, 4, new byte[]{'a', '4'}),
    b4(2, 4, new byte[]{'b', '4'}),
    c4(3, 4, new byte[]{'c', '4'}),
    d4(4, 4, new byte[]{'d', '4'}),
    e4(5, 4, new byte[]{'e', '4'}),
    f4(6, 4, new byte[]{'f', '4'}),
    g4(7, 4, new byte[]{'g', '4'}),
    h4(8, 4, new byte[]{'h', '4'}),

    a3(1, 3, new byte[]{'a', '3'}),
    b3(2, 3, new byte[]{'b', '3'}),
    c3(3, 3, new byte[]{'c', '3'}),
    d3(4, 3, new byte[]{'d', '3'}),
    e3(5, 3, new byte[]{'e', '3'}),
    f3(6, 3, new byte[]{'f', '3'}),
    g3(7, 3, new byte[]{'g', '3'}),
    h3(8, 3, new byte[]{'h', '3'}),

    a2(1, 2, new byte[]{'a', '2'}),
    b2(2, 2, new byte[]{'b', '2'}),
    c2(3, 2, new byte[]{'c', '2'}),
    d2(4, 2, new byte[]{'d', '2'}),
    e2(5, 2, new byte[]{'e', '2'}),
    f2(6, 2, new byte[]{'f', '2'}),
    g2(7, 2, new byte[]{'g', '2'}),
    h2(8, 2, new byte[]{'h', '2'}),

    a1(1, 1, new byte[]{'a', '1'}),
    b1(2, 1, new byte[]{'b', '1'}),
    c1(3, 1, new byte[]{'c', '1'}),
    d1(4, 1, new byte[]{'d', '1'}),
    e1(5, 1, new byte[]{'e', '1'}),
    f1(6, 1, new byte[]{'f', '1'}),
    g1(7, 1, new byte[]{'g', '1'}),
    h1(8, 1, new byte[]{'h', '1'});

    private final int column;

    private final int row;

    private final byte[] bytes;

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

    Coordinate(int column, int row, byte[] bytes) {
        this.column = column;
        this.row = row;
        this.bytes = bytes;
    }

    public int row() {
        return row;
    }

    public int column() {
        return column;
    }

    public byte columnBytes() {
        return bytes[0];
    }

    public byte rowBytes() {
        return bytes[1];
    }

    public long bitMask() {
        return 1L << this.ordinal();
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

    public static @Nullable Coordinate fromBytes(byte file, byte rank) {
        int column = file - 97 + 1;
        int row = rank - 49 + 1;

        return of(row, column);
    }
}
