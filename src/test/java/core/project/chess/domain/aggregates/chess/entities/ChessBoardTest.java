package core.project.chess.domain.aggregates.chess.entities;

import org.junit.jupiter.api.Test;

class ChessBoardTest {

    @Test
    void defaultInitialPosition() {
        ChessBoard chessBoard = ChessBoard.initialPosition();
    }
}