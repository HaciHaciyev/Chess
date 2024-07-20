package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;

public record Knight(Color color)
        implements Piece {

    @Override
    public boolean isValidMove(ChessBoard chessBoard, Field currentField, Field fieldToMove) {
        return false;
    }
}
