package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.Getter;

@Getter
public enum Coordinate {

    A1('A', 1), A2('A', 2), A3('A', 3), A4('A', 4), A5('A', 5), A6('A', 6), A7('A', 7), A8('A', 8),
    B1('B', 1), B2('B', 2), B3('B', 3), B4('B', 4), B5('B', 5), B6('B', 6), B7('B', 7), B8('B', 8),
    C1('C', 1), C2('C', 2), C3('C', 3), C4('C', 4), C5('C', 5), C6('C', 6), C7('C', 7), C8('C', 8),
    D1('D', 1), D2('D', 2), D3('D', 3), D4('D', 4), D5('D', 5), D6('D', 6), D7('D', 7), D8('D', 8),
    E1('E', 1), E2('E', 2), E3('E', 3), E4('E', 4), E5('E', 5), E6('E', 6), E7('E', 7), E8('E', 8),
    F1('F', 1), F2('F', 2), F3('F', 3), F4('F', 4), F5('F', 5), F6('F', 6), F7('F', 7), F8('F', 8),
    G1('G', 1), G2('G', 2), G3('G', 3), G4('G', 4), G5('G', 5), G6('G', 6), G7('G', 7), G8('G', 8),
    H1('H', 1), H2('H', 2), H3('H', 3), H4('H', 4), H5('H', 5), H6('H', 6), H7('H', 7), H8('H', 8);

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
            case 1 -> 'A';
            case 2 -> 'B';
            case 3 -> 'C';
            case 4 -> 'D';
            case 5 -> 'E';
            case 6 -> 'F';
            case 7 -> 'G';
            case 8 -> 'H';
            default -> throw new IllegalStateException("Unexpected value: " + columnNumber);
        };
    }

    public int getColumnAsInt() {
        return switch (column) {
            case 'A' -> 1;
            case 'B' -> 2;
            case 'C' -> 3;
            case 'D' -> 4;
            case 'E' -> 5;
            case 'F' -> 6;
            case 'G' -> 7;
            case 'H' -> 8;
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }
}
