package core.project.chess.domain.chess.enumerations;

import jakarta.annotation.Nullable;

public enum Coordinate {
    a8(1, 8, new byte[]{'a', '8'}, 56),
    b8(2, 8, new byte[]{'b', '8'}, 57),
    c8(3, 8, new byte[]{'c', '8'}, 58),
    d8(4, 8, new byte[]{'d', '8'}, 59),
    e8(5, 8, new byte[]{'e', '8'}, 60),
    f8(6, 8, new byte[]{'f', '8'}, 61),
    g8(7, 8, new byte[]{'g', '8'}, 62),
    h8(8, 8, new byte[]{'h', '8'}, 63),

    a7(1, 7, new byte[]{'a', '7'}, 48),
    b7(2, 7, new byte[]{'b', '7'}, 49),
    c7(3, 7, new byte[]{'c', '7'}, 50),
    d7(4, 7, new byte[]{'d', '7'}, 51),
    e7(5, 7, new byte[]{'e', '7'}, 52),
    f7(6, 7, new byte[]{'f', '7'}, 53),
    g7(7, 7, new byte[]{'g', '7'}, 54),
    h7(8, 7, new byte[]{'h', '7'}, 55),

    a6(1, 6, new byte[]{'a', '6'}, 40),
    b6(2, 6, new byte[]{'b', '6'}, 41),
    c6(3, 6, new byte[]{'c', '6'}, 42),
    d6(4, 6, new byte[]{'d', '6'}, 43),
    e6(5, 6, new byte[]{'e', '6'}, 44),
    f6(6, 6, new byte[]{'f', '6'}, 45),
    g6(7, 6, new byte[]{'g', '6'}, 46),
    h6(8, 6, new byte[]{'h', '6'}, 47),

    a5(1, 5, new byte[]{'a', '5'}, 32),
    b5(2, 5, new byte[]{'b', '5'}, 33),
    c5(3, 5, new byte[]{'c', '5'}, 34),
    d5(4, 5, new byte[]{'d', '5'}, 35),
    e5(5, 5, new byte[]{'e', '5'}, 36),
    f5(6, 5, new byte[]{'f', '5'}, 37),
    g5(7, 5, new byte[]{'g', '5'}, 38),
    h5(8, 5, new byte[]{'h', '5'}, 39),

    a4(1, 4, new byte[]{'a', '4'}, 24),
    b4(2, 4, new byte[]{'b', '4'}, 25),
    c4(3, 4, new byte[]{'c', '4'}, 26),
    d4(4, 4, new byte[]{'d', '4'}, 27),
    e4(5, 4, new byte[]{'e', '4'}, 28),
    f4(6, 4, new byte[]{'f', '4'}, 29),
    g4(7, 4, new byte[]{'g', '4'}, 30),
    h4(8, 4, new byte[]{'h', '4'}, 31),

    a3(1, 3, new byte[]{'a', '3'}, 16),
    b3(2, 3, new byte[]{'b', '3'}, 17),
    c3(3, 3, new byte[]{'c', '3'}, 18),
    d3(4, 3, new byte[]{'d', '3'}, 19),
    e3(5, 3, new byte[]{'e', '3'}, 20),
    f3(6, 3, new byte[]{'f', '3'}, 21),
    g3(7, 3, new byte[]{'g', '3'}, 22),
    h3(8, 3, new byte[]{'h', '3'}, 23),

    a2(1, 2, new byte[]{'a', '2'}, 8),
    b2(2, 2, new byte[]{'b', '2'}, 9),
    c2(3, 2, new byte[]{'c', '2'}, 10),
    d2(4, 2, new byte[]{'d', '2'}, 11),
    e2(5, 2, new byte[]{'e', '2'}, 12),
    f2(6, 2, new byte[]{'f', '2'}, 13),
    g2(7, 2, new byte[]{'g', '2'}, 14),
    h2(8, 2, new byte[]{'h', '2'}, 15),

    a1(1, 1, new byte[]{'a', '1'}, 0),
    b1(2, 1, new byte[]{'b', '1'}, 1),
    c1(3, 1, new byte[]{'c', '1'}, 2),
    d1(4, 1, new byte[]{'d', '1'}, 3),
    e1(5, 1, new byte[]{'e', '1'}, 4),
    f1(6, 1, new byte[]{'f', '1'}, 5),
    g1(7, 1, new byte[]{'g', '1'}, 6),
    h1(8, 1, new byte[]{'h', '1'}, 7);

    private final int column;

    private final int row;

    private final byte[] notationsBytes;

    private final int index;

    private static final Coordinate[] COORDINATES = {
            a1, b1, c1, d1, e1, f1, g1, h1,
            a2, b2, c2, d2, e2, f2, g2, h2,
            a3, b3, c3, d3, e3, f3, g3, h3,
            a4, b4, c4, d4, e4, f4, g4, h4,
            a5, b5, c5, d5, e5, f5, g5, h5,
            a6, b6, c6, d6, e6, f6, g6, h6,
            a7, b7, c7, d7, e7, f7, g7, h7,
            a8, b8, c8, d8, e8, f8, g8, h8
    };
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

    Coordinate(int column, int row, byte[] notationsBytes, int index) {
        this.column = column;
        this.row = row;
        this.notationsBytes = notationsBytes;
        this.index = index;
    }

    public int row() {
        return row;
    }

    public int column() {
        return column;
    }

    public byte columnNotationBytes() {
        return notationsBytes[0];
    }

    public byte rowNotationBytes() {
        return notationsBytes[1];
    }

    public int index() {
        return index;
    }

    public long bitMask() {
        return 1L << this.index;
    }

    /**
     * Finds the Coordinate object corresponding to the given row and column.
     *
     * @param row    the row number (1-8)
     * @param column the column number (1-8)
     * @return a Coordinate containing the Coordinate object if the row and column are valid,
     * or a null with a false status if the row or column is out of bounds
     */
    @Nullable
    public static Coordinate of(int row, int column) {
        if (row > 8 || column > 8 || row < 1 || column < 1) {
            return null;
        }

        return COORDINATE_CACHE[row - 1][column - 1];
    }

    @Nullable
    public static Coordinate fromNotationBytes(byte file, byte rank) {
        int column = file - 97 + 1;
        int row = rank - 49 + 1;

        return of(row, column);
    }

    public static Coordinate byIndex(int index) {
        if (index < 0 || index >= COORDINATES.length) return null;
        return COORDINATES[index];
    }
}
