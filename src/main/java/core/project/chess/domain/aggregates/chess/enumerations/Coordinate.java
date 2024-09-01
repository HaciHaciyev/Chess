package core.project.chess.domain.aggregates.chess.enumerations;

import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.Getter;

@Getter
public enum Coordinate {

    A8('a', 8), B8('b', 8), C8('c', 8), D8('d', 8), E8('e', 8), F8('f', 8), G8('g', 8), H8('h', 8),
    A7('a', 7), B7('b', 7), C7('c', 7), D7('d', 7), E7('e', 7), F7('f', 7), G7('g', 7), H7('h', 7),
    A6('a', 6), B6('b', 6), C6('c', 6), D6('d', 6), E6('e', 6), F6('f', 6), G6('g', 6), H6('h', 6),
    A5('a', 5), B5('b', 5), C5('c', 5), D5('d', 5), E5('e', 5), F5('f', 5), G5('g', 5), H5('h', 5),
    A4('a', 4), B4('b', 4), C4('c', 4), D4('d', 4), E4('e', 4), F4('f', 4), G4('g', 4), H4('h', 4),
    A3('a', 3), B3('b', 3), C3('c', 3), D3('d', 3), E3('e', 3), F3('f', 3), G3('g', 3), H3('h', 3),
    A2('a', 2), B2('b', 2), C2('c', 2), D2('d', 2), E2('e', 2), F2('f', 2), G2('g', 2), H2('h', 2),
    A1('a', 1), B1('b', 1), C1('c', 1), D1('d', 1), E1('e', 1), F1('f', 1), G1('g', 1), H1('h', 1);

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
