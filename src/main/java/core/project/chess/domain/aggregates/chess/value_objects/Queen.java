package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;

public record Queen(Color color)
        implements Piece {

    @Override
    public boolean isValidMove(ChessBoard chessBoard, Coordinate from, Coordinate to) {
        return false;
    }
}
