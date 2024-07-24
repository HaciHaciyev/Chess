package core.project.chess.domain.aggregates.chess.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class ChessBoardTest {

    @Test
    void defaultInitialPosition() {
        ChessBoard chessBoard = ChessBoard.initialPosition(UUID.randomUUID());
    }
}