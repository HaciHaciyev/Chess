package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessBoardFENGenerationTest {

    @Test
    void testFENGeneration() throws InterruptedException {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        //1.
        chessBoard.doMove( e2, e4, null);
        Thread.sleep(Duration.ofSeconds(1));
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", chessBoard.toString());
        Log.info(chessBoard.toString());

        chessBoard.doMove( e7, e5, null);
        Log.info(chessBoard.toString());

        //2.
        chessBoard.doMove( g1, f3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( b8, c6, null);
        Log.info(chessBoard.toString());

        //3.
        chessBoard.doMove( b1, c3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( g8, f6, null);
        Log.info(chessBoard.toString());

        //4.
        chessBoard.doMove( d2, d3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( f8, c5, null);
        Log.info(chessBoard.toString());

        //5.
        chessBoard.doMove( c1, g5, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( h7, h6, null);
        Log.info(chessBoard.toString());

        //6.
        chessBoard.doMove( g5, f6, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d8, f6, null);
        Log.info(chessBoard.toString());

        //7.
        chessBoard.doMove( a2, a3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d7, d6, null);
        Log.info(chessBoard.toString());

        //8.
        chessBoard.doMove( c3, d5, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( f6, d8, null);
        Log.info(chessBoard.toString());

        //9.
        chessBoard.doMove( c2, c3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c8, e6, null);
        Log.info(chessBoard.toString());

        //10.
        chessBoard.doMove( d1, a4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d8, d7, null);
        Log.info(chessBoard.toString());

        //11.
        chessBoard.doMove( d5, e3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c5, e3, null);
        Log.info(chessBoard.toString());

        //12.
        chessBoard.doMove( f2, e3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( e8, g8, null);
        Log.info(chessBoard.toString());

        //13.
        chessBoard.doMove( d3, d4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( e6, g4, null);
        Log.info(chessBoard.toString());

        //14.
        chessBoard.doMove( f3, d2, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d7, e7, null);
        Log.info(chessBoard.toString());

        //15.
        chessBoard.doMove( d4, e5, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d6, e5, null);
        Log.info(chessBoard.toString());

        //16.
        chessBoard.doMove( d2, c4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( e7, g5, null);
        Log.info(chessBoard.toString());

        //17.
        chessBoard.doMove( f1, d3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( g4, e6, null);
        Log.info(chessBoard.toString());

        //18.
        chessBoard.doMove( g2, g3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( e6, c4, null);
        Log.info(chessBoard.toString());

        //19.
        chessBoard.doMove( d3, c4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( g5, e3, null);
        Log.info(chessBoard.toString());

        //20.
        chessBoard.doMove( c4, e2, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( a7, a6, null);
        Log.info(chessBoard.toString());

        //21.
        chessBoard.doMove( h1, f1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( b7, b5, null);
        Log.info(chessBoard.toString());

        //22.
        chessBoard.doMove( a4, c2, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( a6, a5, null);
        Log.info(chessBoard.toString());

        //23.
        chessBoard.doMove( c3, c4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( b5, b4, null);
        Log.info(chessBoard.toString());

        //24.
        chessBoard.doMove( a3, b4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c6, b4, null);
        Log.info(chessBoard.toString());

        //25.
        chessBoard.doMove( c2, b1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( f8, d8, null);
        Log.info(chessBoard.toString());

        //26.
        chessBoard.doMove( f1, f5, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( f7, f6, null);
        Log.info(chessBoard.toString());

        //27.
        chessBoard.doMove( a1, a3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( e3, d2, null);
        Log.info(chessBoard.toString());

        //28.
        chessBoard.doMove( e1, f1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d2, c2, null);
        Log.info(chessBoard.toString());

        //29.
        chessBoard.doMove( a3, a1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d8, d4, null);
        Log.info(chessBoard.toString());

        //30.
        chessBoard.doMove( e2, f3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c2, c4, null);
        Log.info(chessBoard.toString());

        //31.
        chessBoard.doMove( f1, g2, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d4, d2, null);
        Log.info(chessBoard.toString());

        //32.
        chessBoard.doMove( g2, g1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( g8, f7, null);
        Log.info(chessBoard.toString());

        //33.
        chessBoard.doMove( b1, f1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c4, f1, null);
        Log.info(chessBoard.toString());

        //34.
        chessBoard.doMove( g1, f1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( g7, g6, null);
        Log.info(chessBoard.toString());

        //35.
        chessBoard.doMove( f1, e1, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d2, b2, null);
        Log.info(chessBoard.toString());

        //36.
        chessBoard.doMove( f3, h5, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( b4, c2, null);
        Log.info(chessBoard.toString());

        //37.
        chessBoard.doMove( e1, f2, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c2, a1, null);
        Log.info(chessBoard.toString());

        //38.
        chessBoard.doMove( f2, f3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( a8, d8, null);
        Log.info(chessBoard.toString());

        //39.
        chessBoard.doMove( f3, g4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c7, c5, null);
        Log.info(chessBoard.toString());

        //40.
        chessBoard.doMove( h2, h4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c5, c4, null);
        Log.info(chessBoard.toString());

        //41.
        chessBoard.doMove( g4, f3, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( c4, c3, null);
        Log.info(chessBoard.toString());

        //42. checkmate
        chessBoard.doMove( g3, g4, null);
        Log.info(chessBoard.toString());

        chessBoard.doMove( d8, d3, null);
        Log.info(chessBoard.toString());
    }
}
