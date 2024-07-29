package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.infrastructure.utilities.StatusPair;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record King(Color color)
        implements Piece {

    @Override
    public StatusPair<Operations> isValidMove(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return StatusPair.ofFalse();
    }

    public StatusPair<Operations> canCastle(ChessBoard chessBoard, Field from, Field to) {
        return StatusPair.ofFalse();
    }
}
