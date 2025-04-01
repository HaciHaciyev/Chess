package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ZobristHashKeysTest {

    @Test
    void equalsForTheSamePosition() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ChessBoard chessBoard1 = ChessBoard.starndardChessBoard();
        assertEqualsAndLog(chessBoard, chessBoard1);

        chessBoard1.reposition(Coordinate.e2, Coordinate.e4);
        assertNonEqualsAndLog(chessBoard, chessBoard1);

        chessBoard1.returnOfTheMovement();
        assertEqualsAndLog(chessBoard, chessBoard1);

        ChessBoard chessBoard2 = ChessBoard.fromPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        ChessBoard chessBoard3 = ChessBoard.fromPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertEqualsAndLog(chessBoard2, chessBoard3);
    }

    @Test
    void moves() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ChessBoard chessBoard1 = ChessBoard.starndardChessBoard();

        moveAndAssertEquals(Coordinate.e2, Coordinate.e4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e7, Coordinate.e5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g1, Coordinate.f3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.b8, Coordinate.c6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.b1, Coordinate.c3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g8, Coordinate.f6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f1, Coordinate.c4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d7, Coordinate.d6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d2, Coordinate.d3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.c8, Coordinate.e6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e1, Coordinate.g1, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e6, Coordinate.c4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d3, Coordinate.c4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f8, Coordinate.e7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d1, Coordinate.e2, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d8, Coordinate.d7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.c1, Coordinate.e3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e8, Coordinate.c8, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.a1, Coordinate.d1, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f6, Coordinate.g4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.c3, Coordinate.d5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h7, Coordinate.h6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.b2, Coordinate.b3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e7, Coordinate.g5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h2, Coordinate.h3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g4, Coordinate.e3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f2, Coordinate.e3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.a7, Coordinate.a6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h3, Coordinate.h4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g5, Coordinate.f6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f3, Coordinate.h2, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f6, Coordinate.h4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f1, Coordinate.f3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h8, Coordinate.f8, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f3, Coordinate.f5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g7, Coordinate.g6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f5, Coordinate.f3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f7, Coordinate.f5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f3, Coordinate.h3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g6, Coordinate.g5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e2, Coordinate.h5, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f5, Coordinate.e4, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h5, Coordinate.h6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f8, Coordinate.f2, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g2, Coordinate.g3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d7, Coordinate.h3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d5, Coordinate.e7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.c6, Coordinate.e7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g1, Coordinate.f2, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h3, Coordinate.h2, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.f2, Coordinate.f1, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h2, Coordinate.g3, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.h6, Coordinate.e6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d8, Coordinate.d7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e6, Coordinate.g8, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.d7, Coordinate.d8, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g8, Coordinate.e6, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.c8, Coordinate.b8, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.e6, Coordinate.e7, chessBoard, chessBoard1);
        moveAndAssertEquals(Coordinate.g3, Coordinate.f2, chessBoard, chessBoard1);
    }

    private static void moveAndAssertEquals(Coordinate from, Coordinate to, ChessBoard chessBoard, ChessBoard chessBoard1) {
        chessBoard.reposition(from, to);
        chessBoard1.reposition(from, to);
        assertEqualsAndLog(chessBoard, chessBoard1);
    }

    private static void assertEqualsAndLog(ChessBoard chessBoard, ChessBoard chessBoard1) {
        assertEquals(chessBoard.zobristHash(), chessBoard1.zobristHash());
        Log.infof("First hash: %d. Second hash: %d.", chessBoard.zobristHash(), chessBoard1.zobristHash());
    }

    private void assertNonEqualsAndLog(ChessBoard chessBoard, ChessBoard chessBoard1) {
        assertNotEquals(chessBoard.zobristHash(), chessBoard1.zobristHash());
        Log.infof("First hash: %d. Second hash: %d.", chessBoard.zobristHash(), chessBoard1.zobristHash());
    }
}