package core.project.chess.domain.aggregates.chess.enumerations;

import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.Getter;

@Getter
public enum Coordinate {

    A1('a', 1), A2('a', 2), A3('a', 3), A4('a', 4), A5('a', 5), A6('a', 6), A7('a', 7), A8('a', 8),
    B1('b', 1), B2('b', 2), B3('b', 3), B4('b', 4), B5('b', 5), B6('b', 6), B7('b', 7), B8('b', 8),
    C1('c', 1), C2('c', 2), C3('c', 3), C4('c', 4), C5('c', 5), C6('c', 6), C7('c', 7), C8('c', 8),
    D1('d', 1), D2('d', 2), D3('d', 3), D4('d', 4), D5('d', 5), D6('d', 6), D7('d', 7), D8('d', 8),
    E1('e', 1), E2('e', 2), E3('e', 3), E4('e', 4), E5('e', 5), E6('e', 6), E7('e', 7), E8('e', 8),
    F1('f', 1), F2('f', 2), F3('f', 3), F4('f', 4), F5('f', 5), F6('f', 6), F7('f', 7), F8('f', 8),
    G1('g', 1), G2('g', 2), G3('g', 3), G4('g', 4), G5('g', 5), G6('g', 6), G7('g', 7), G8('g', 8),
    H1('h', 1), H2('h', 2), H3('h', 3), H4('h', 4), H5('h', 5), H6('h', 6), H7('h', 7), H8('h', 8);

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
    public static StatusPair<Coordinate> coordinate(int row, int column) {
        if (row > 8 || column > 8 || row < 1 || column < 1) {
            return StatusPair.ofFalse();
        }

        final char charColumn = intToColumn(column);

        for (Coordinate coordinate : Coordinate.values()) {
            if (coordinate.row == row && coordinate.column == charColumn) {
                return StatusPair.ofTrue(coordinate);
            }
        }

        return StatusPair.ofFalse();
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

    public int getColumnAsInt() {
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
