package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();

    /** Fully validates the move, and also returns
     * a list of operations that the given operation can perform,
     * in the order in which they should be performed.*/
    StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to);

    default int columnToInt(char startColumn) {
        return switch (startColumn) {
            case 'A' -> 1;
            case 'B' -> 2;
            case 'C' -> 3;
            case 'D' -> 4;
            case 'E' -> 5;
            case 'F' -> 6;
            case 'G' -> 7;
            case 'H' -> 8;
            default -> throw new IllegalStateException("Unexpected value: " + startColumn);
        };
    }

    default Operations influenceOnTheOpponentKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        if (chessBoard.shouldPutStalemate(from, to)) {
            return Operations.STALEMATE;
        }
        if (chessBoard.shouldPutCheckmate(from, to)) {
            return Operations.CHECKMATE;
        }
        if (chessBoard.shouldPutCheck(from, to)) {
            return Operations.CHECK;
        }
        
        return Operations.EMPTY;
    }
}

