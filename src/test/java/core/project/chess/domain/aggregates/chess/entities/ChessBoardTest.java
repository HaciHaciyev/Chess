package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ChessBoardTest {

    @Test
    void defaultStandardChessBoard() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());

        chessBoard.reposition(Coordinate.D2, Coordinate.D4, null);
    }
}