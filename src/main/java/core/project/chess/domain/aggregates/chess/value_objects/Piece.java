package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    boolean isValidMove(ChessBoard chessBoard, Coordinate from, Coordinate to);
}

