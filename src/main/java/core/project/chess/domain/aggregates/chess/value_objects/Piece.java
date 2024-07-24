package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();
    
    StatusPair<Operations> isValidMove(ChessBoard chessBoard, Coordinate from, Coordinate to);
}

