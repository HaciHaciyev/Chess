package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.StatusPair;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public sealed interface Piece
        permits Bishop, King, Knight, Pawn, Queen, Rook {

    Color color();

    /** Can return only operations like EMPTY, CAPTURE, PROMOTION(in case of Pawn)*/
    StatusPair<Operations> isValidMove(ChessBoard chessBoard, Coordinate from, Coordinate to);
}

