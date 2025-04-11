package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.enumerations.Direction;

public class BitboardUtils {

    public static long rayMask(Direction direction, int fromSquare) {
        long ray = 0L;
        int row = fromSquare / 8;
        int col = fromSquare % 8;

        int rowStep = -direction.rowDelta();
        int colStep = direction.colDelta();

        row += rowStep;
        col += colStep;

        while (row >= 0 && row < 8 && col >= 0 && col < 8) {
            int index = row * 8 + col;
            ray |= (1L << index);

            row += rowStep;
            col += colStep;
        }

        return ray;
    }
}
