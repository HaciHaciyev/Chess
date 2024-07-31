package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.LinkedHashSet;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record King(Color color)
        implements Piece {

    @Override
    public StatusPair<LinkedHashSet<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        return StatusPair.ofFalse();
    }

    public StatusPair<LinkedHashSet<Operations>> canCastle(ChessBoard chessBoard, Field from, Field to) {
        return StatusPair.ofFalse();
    }

    /** TODO for Nicat*/
    public boolean safeForKing(ChessBoard chessBoard, Coordinate kingPosition, Coordinate from, Coordinate to) {
        return false;
    }

    /** TODO for Nicat*/
    public boolean stalemate(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }

    /** TODO for Nicat*/
    public boolean checkmate(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }

    /** TODO for Nicat*/
    public boolean check(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }
}
