package core.project.chess.domain.aggregates.chess.enumerations;

import core.project.chess.infrastructure.utilities.containers.StatusPair;
import lombok.Getter;

@Getter
public enum Coordinate {

    a8('a', 8), b8('b', 8), c8('c', 8), d8('d', 8), e8('e', 8), f8('f', 8), g8('g', 8), h8('h', 8),
    a7('a', 7), b7('b', 7), c7('c', 7), d7('d', 7), e7('e', 7), f7('f', 7), g7('g', 7), h7('h', 7),
    a6('a', 6), b6('b', 6), c6('c', 6), d6('d', 6), e6('e', 6), f6('f', 6), g6('g', 6), h6('h', 6),
    a5('a', 5), b5('b', 5), c5('c', 5), d5('d', 5), e5('e', 5), f5('f', 5), g5('g', 5), h5('h', 5),
    a4('a', 4), b4('b', 4), c4('c', 4), d4('d', 4), e4('e', 4), f4('f', 4), g4('g', 4), h4('h', 4),
    a3('a', 3), b3('b', 3), c3('c', 3), d3('d', 3), e3('e', 3), f3('f', 3), g3('g', 3), h3('h', 3),
    a2('a', 2), b2('b', 2), c2('c', 2), d2('d', 2), e2('e', 2), f2('f', 2), g2('g', 2), h2('h', 2),
    a1('a', 1), b1('b', 1), c1('c', 1), d1('d', 1), e1('e', 1), f1('f', 1), g1('g', 1), h1('h', 1);

    private final int row;

    private final char column;

    Coordinate(char column, int row) {
        this.column = column;
        this.row = row;
    }

    /**
     * Finds the Coordinate object corresponding to the given row and column.
     *
     * @param row    the row number (1-8)
     * @param column the column number (1-8)
     * @return a StatusPair containing the Coordinate object if the row and column are valid, or a StatusPair with a false status if the row or column is out of bounds
     */
    public static StatusPair<Coordinate> of(int row, int column) {
        if (row > 8 || column > 8 || row < 1 || column < 1) {
            return StatusPair.ofFalse();
        }

        return StatusPair.ofTrue(Coordinate.valueOf(String.valueOf(intToColumn(column)) + row));
    }

    /**
     * Converts a column number (1-8) to the corresponding column letter (A-H).
     *
     * @param columnNumber the column number (1-8)
     * @return the corresponding column letter (A-H)
     * @throws IllegalStateException if the columnNumber is not between 1 and 8 (inclusive)
     */
    public static char intToColumn(int columnNumber) {
        return switch (columnNumber) {
            case 1 -> 'a';
            case 2 -> 'b';
            case 3 -> 'c';
            case 4 -> 'd';
            case 5 -> 'e';
            case 6 -> 'f';
            case 7 -> 'g';
            case 8 -> 'h';
            default -> throw new IllegalStateException("Unexpected value: " + columnNumber);
        };
    }

    public int columnToInt() {
        return switch (column) {
            case 'a' -> 1;
            case 'b' -> 2;
            case 'c' -> 3;
            case 'd' -> 4;
            case 'e' -> 5;
            case 'f' -> 6;
            case 'g' -> 7;
            case 'h' -> 8;
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }
}
