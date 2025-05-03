package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessGame;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static core.project.chess.domain.entities.ChessGameTest.chessGameSupplier;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessBoardFENGenerationTest {

    @Test
    void testFENGeneration() throws InterruptedException {
        ChessGame chessGame = chessGameSupplier().get();
        String firstPlayer = chessGame.whitePlayer().username();
        String secondPlayer = chessGame.blackPlayer().username();

        //1.
        chessGame.doMove(firstPlayer, e2, e4, null);
        Thread.sleep(Duration.ofSeconds(1));
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", chessGame.fen());
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e7, e5, null);
        Log.info(chessGame.fen());

        //2.
        chessGame.doMove(firstPlayer, g1, f3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, b8, c6, null);
        Log.info(chessGame.fen());

        //3.
        chessGame.doMove(firstPlayer, b1, c3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, g8, f6, null);
        Log.info(chessGame.fen());

        //4.
        chessGame.doMove(firstPlayer, d2, d3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, f8, c5, null);
        Log.info(chessGame.fen());

        //5.
        chessGame.doMove(firstPlayer, c1, g5, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, h7, h6, null);
        Log.info(chessGame.fen());

        //6.
        chessGame.doMove(firstPlayer, g5, f6, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d8, f6, null);
        Log.info(chessGame.fen());

        //7.
        chessGame.doMove(firstPlayer, a2, a3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d7, d6, null);
        Log.info(chessGame.fen());

        //8.
        chessGame.doMove(firstPlayer, c3, d5, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, f6, d8, null);
        Log.info(chessGame.fen());

        //9.
        chessGame.doMove(firstPlayer, c2, c3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c8, e6, null);
        Log.info(chessGame.fen());

        //10.
        chessGame.doMove(firstPlayer, d1, a4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d8, d7, null);
        Log.info(chessGame.fen());

        //11.
        chessGame.doMove(firstPlayer, d5, e3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c5, e3, null);
        Log.info(chessGame.fen());

        //12.
        chessGame.doMove(firstPlayer, f2, e3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e8, g8, null);
        Log.info(chessGame.fen());

        //13.
        chessGame.doMove(firstPlayer, d3, d4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e6, g4, null);
        Log.info(chessGame.fen());

        //14.
        chessGame.doMove(firstPlayer, f3, d2, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d7, e7, null);
        Log.info(chessGame.fen());

        //15.
        chessGame.doMove(firstPlayer, d4, e5, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d6, e5, null);
        Log.info(chessGame.fen());

        //16.
        chessGame.doMove(firstPlayer, d2, c4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e7, g5, null);
        Log.info(chessGame.fen());

        //17.
        chessGame.doMove(firstPlayer, f1, d3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, g4, e6, null);
        Log.info(chessGame.fen());

        //18.
        chessGame.doMove(firstPlayer, g2, g3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e6, c4, null);
        Log.info(chessGame.fen());

        //19.
        chessGame.doMove(firstPlayer, d3, c4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, g5, e3, null);
        Log.info(chessGame.fen());

        //20.
        chessGame.doMove(firstPlayer, c4, e2, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, a7, a6, null);
        Log.info(chessGame.fen());

        //21.
        chessGame.doMove(firstPlayer, h1, f1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, b7, b5, null);
        Log.info(chessGame.fen());

        //22.
        chessGame.doMove(firstPlayer, a4, c2, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, a6, a5, null);
        Log.info(chessGame.fen());

        //23.
        chessGame.doMove(firstPlayer, c3, c4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, b5, b4, null);
        Log.info(chessGame.fen());

        //24.
        chessGame.doMove(firstPlayer, a3, b4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c6, b4, null);
        Log.info(chessGame.fen());

        //25.
        chessGame.doMove(firstPlayer, c2, b1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, f8, d8, null);
        Log.info(chessGame.fen());

        //26.
        chessGame.doMove(firstPlayer, f1, f5, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, f7, f6, null);
        Log.info(chessGame.fen());

        //27.
        chessGame.doMove(firstPlayer, a1, a3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, e3, d2, null);
        Log.info(chessGame.fen());

        //28.
        chessGame.doMove(firstPlayer, e1, f1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d2, c2, null);
        Log.info(chessGame.fen());

        //29.
        chessGame.doMove(firstPlayer, a3, a1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d8, d4, null);
        Log.info(chessGame.fen());

        //30.
        chessGame.doMove(firstPlayer, e2, f3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c2, c4, null);
        Log.info(chessGame.fen());

        //31.
        chessGame.doMove(firstPlayer, f1, g2, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d4, d2, null);
        Log.info(chessGame.fen());

        //32.
        chessGame.doMove(firstPlayer, g2, g1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, g8, f7, null);
        Log.info(chessGame.fen());

        //33.
        chessGame.doMove(firstPlayer, b1, f1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c4, f1, null);
        Log.info(chessGame.fen());

        //34.
        chessGame.doMove(firstPlayer, g1, f1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, g7, g6, null);
        Log.info(chessGame.fen());

        //35.
        chessGame.doMove(firstPlayer, f1, e1, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d2, b2, null);
        Log.info(chessGame.fen());

        //36.
        chessGame.doMove(firstPlayer, f3, h5, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, b4, c2, null);
        Log.info(chessGame.fen());

        //37.
        chessGame.doMove(firstPlayer, e1, f2, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c2, a1, null);
        Log.info(chessGame.fen());

        //38.
        chessGame.doMove(firstPlayer, f2, f3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, a8, d8, null);
        Log.info(chessGame.fen());

        //39.
        chessGame.doMove(firstPlayer, f3, g4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c7, c5, null);
        Log.info(chessGame.fen());

        //40.
        chessGame.doMove(firstPlayer, h2, h4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c5, c4, null);
        Log.info(chessGame.fen());

        //41.
        chessGame.doMove(firstPlayer, g4, f3, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, c4, c3, null);
        Log.info(chessGame.fen());

        //42. checkmate
        chessGame.doMove(firstPlayer, g3, g4, null);
        Log.info(chessGame.fen());

        chessGame.doMove(secondPlayer, d8, d3, null);
        Log.info(chessGame.fen());
    }
}
