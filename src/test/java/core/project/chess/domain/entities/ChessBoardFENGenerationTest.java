package core.project.chess.domain.entities;

import com.esotericsoftware.minlog.Log;
import core.project.chess.domain.chess.entities.ChessGame;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static core.project.chess.domain.chess.enumerations.Coordinate.d3;
import static core.project.chess.domain.entities.ChessGameTest.defaultChessGameSupplier;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessBoardFENGenerationTest {

    @Test
    void testFENGeneration() throws InterruptedException {
        ChessGame chessGame = defaultChessGameSupplier().get();
        String firstPlayer = chessGame.getPlayerForWhite().getUsername().username();
        String secondPlayer = chessGame.getPlayerForBlack().getUsername().username();

        //1.
        chessGame.makeMovement(firstPlayer, e2, e4, null);
        Thread.sleep(Duration.ofSeconds(1));
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", chessGame.getChessBoard().actualRepresentationOfChessBoard());
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e7, e5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //2.
        chessGame.makeMovement(firstPlayer, g1, f3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, b8, c6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //3.
        chessGame.makeMovement(firstPlayer, b1, c3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, g8, f6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //4.
        chessGame.makeMovement(firstPlayer, d2, d3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, f8, c5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //5.
        chessGame.makeMovement(firstPlayer, c1, g5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, h7, h6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //6.
        chessGame.makeMovement(firstPlayer, g5, f6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d8, f6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //7.
        chessGame.makeMovement(firstPlayer, a2, a3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d7, d6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //8.
        chessGame.makeMovement(firstPlayer, c3, d5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, f6, d8, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //9.
        chessGame.makeMovement(firstPlayer, c2, c3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c8, e6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //10.
        chessGame.makeMovement(firstPlayer, d1, a4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d8, d7, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //11.
        chessGame.makeMovement(firstPlayer, d5, e3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c5, e3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //12.
        chessGame.makeMovement(firstPlayer, f2, e3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e8, g8, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //13.
        chessGame.makeMovement(firstPlayer, d3, d4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e6, g4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //14.
        chessGame.makeMovement(firstPlayer, f3, d2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d7, e7, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //15.
        chessGame.makeMovement(firstPlayer, d4, e5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d6, e5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //16.
        chessGame.makeMovement(firstPlayer, d2, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e7, g5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //17.
        chessGame.makeMovement(firstPlayer, f1, d3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, g4, e6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //18.
        chessGame.makeMovement(firstPlayer, g2, g3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e6, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //19.
        chessGame.makeMovement(firstPlayer, d3, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, g5, e3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //20.
        chessGame.makeMovement(firstPlayer, c4, e2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, a7, a6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //21.
        chessGame.makeMovement(firstPlayer, h1, f1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, b7, b5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //22.
        chessGame.makeMovement(firstPlayer, a4, c2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, a6, a5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //23.
        chessGame.makeMovement(firstPlayer, c3, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, b5, b4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //24.
        chessGame.makeMovement(firstPlayer, a3, b4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c6, b4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //25.
        chessGame.makeMovement(firstPlayer, c2, b1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, f8, d8, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //26.
        chessGame.makeMovement(firstPlayer, f1, f5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, f7, f6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //27.
        chessGame.makeMovement(firstPlayer, a1, a3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, e3, d2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //28.
        chessGame.makeMovement(firstPlayer, e1, f1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d2, c2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //29.
        chessGame.makeMovement(firstPlayer, a3, a1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d8, d4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //30.
        chessGame.makeMovement(firstPlayer, e2, f3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c2, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //31.
        chessGame.makeMovement(firstPlayer, f1, g2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d4, d2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //32.
        chessGame.makeMovement(firstPlayer, g2, g1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, g8, f7, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //33.
        chessGame.makeMovement(firstPlayer, b1, f1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c4, f1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //34.
        chessGame.makeMovement(firstPlayer, g1, f1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, g7, g6, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //35.
        chessGame.makeMovement(firstPlayer, f1, e1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d2, b2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //36.
        chessGame.makeMovement(firstPlayer, f3, h5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, b4, c2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //37.
        chessGame.makeMovement(firstPlayer, e1, f2, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c2, a1, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //38.
        chessGame.makeMovement(firstPlayer, f2, f3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, a8, d8, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //39.
        chessGame.makeMovement(firstPlayer, f3, g4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c7, c5, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //40.
        chessGame.makeMovement(firstPlayer, h2, h4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c5, c4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //41.
        chessGame.makeMovement(firstPlayer, g4, f3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, c4, c3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        //42. checkmate
        chessGame.makeMovement(firstPlayer, g3, g4, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());

        chessGame.makeMovement(secondPlayer, d8, d3, null);
        Log.info(chessGame.getChessBoard().actualRepresentationOfChessBoard());
    }
}
