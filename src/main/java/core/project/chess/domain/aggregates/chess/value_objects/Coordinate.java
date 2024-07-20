package core.project.chess.domain.aggregates.chess.value_objects;

import lombok.Getter;

@Getter
public enum Coordinate {

    A1(1, 'A'), A2(2, 'A'), A3(3, 'A'), A4(4, 'A'), A5(5, 'A'), A6(6, 'A'), A7(7, 'A'), A8(8, 'A'),
    B1(1, 'B'), B2(2, 'B'), B3(3, 'B'), B4(4, 'B'), B5(5, 'B'), B6(6, 'B'), B7(7, 'B'), B8(8, 'B'),
    C1(1, 'C'), C2(2, 'C'), C3(3, 'C'), C4(4, 'C'), C5(5, 'C'), C6(6, 'C'), C7(7, 'C'), C8(8, 'C'),
    D1(1, 'D'), D2(2, 'D'), D3(3, 'D'), D4(4, 'D'), D5(5, 'D'), D6(6, 'D'), D7(7, 'D'), D8(8, 'D'),
    E1(1, 'E'), E2(2, 'E'), E3(3, 'E'), E4(4, 'E'), E5(5, 'E'), E6(6, 'E'), E7(7, 'E'), E8(8, 'E'),
    F1(1, 'F'), F2(2, 'F'), F3(3, 'F'), F4(4, 'F'), F5(5, 'F'), F6(6, 'F'), F7(7, 'F'), F8(8, 'F'),
    G1(1, 'G'), G2(2, 'G'), G3(3, 'G'), G4(4, 'G'), G5(5, 'G'), G6(6, 'G'), G7(7, 'G'), G8(8, 'G'),
    H1(1, 'H'), H2(2, 'H'), H3(3, 'H'), H4(4, 'H'), H5(5, 'H'), H6(6, 'H'), H7(7, 'H'), H8(8, 'H');

    private final int row;

    private final char column;

    Coordinate(int row, char column) {
        this.row = row;
        this.column = column;
    }

}
