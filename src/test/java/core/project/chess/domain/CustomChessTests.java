package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.pieces.Queen;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import org.junit.jupiter.api.Test;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static core.project.chess.domain.entities.ChessGameTest.chessGameSupplier;

class CustomChessTests {

    @Test
    void revertPromotion() {
        ChessGame game = chessGameSupplier().get();
        ChessBoardNavigator navigator = new ChessBoardNavigator(game.getChessBoard());

        String whitePlayer = game.getPlayerForWhite().getUsername();
        String blackPlayer = game.getPlayerForBlack().getUsername();

        //1.
        game.makeMovement(whitePlayer, e2, e4, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, e7, e5, null);
        System.out.println(navigator.prettyToString());

        //2.
        game.makeMovement(whitePlayer, f2, f4, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, e5, f4, null);
        System.out.println(navigator.prettyToString());

        //3.
        game.makeMovement(whitePlayer, g2, g3, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, f4, g3, null);
        System.out.println(navigator.prettyToString());

        //4.
        game.makeMovement(whitePlayer, g1, f3, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, g3, h2, null);
        System.out.println(navigator.prettyToString());

        //5.
        game.makeMovement(whitePlayer, f3, g1, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, h2, g1, Queen.of(Color.BLACK));
        System.out.println(navigator.prettyToString());

        game.returnMovement(whitePlayer);
        game.returnMovement(blackPlayer);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, d8, h4, Queen.of(Color.BLACK));
        System.out.println(navigator.prettyToString());
    }

    @Test
    void enPassaunOnCheck() {
        ChessGame chessGame = chessGameSupplier("8/1p3p1k/1P2b2p/2Q1P1pP/5K1n/8/5P2/4r3 w - g6 0 63").get();
        ChessBoardNavigator navigator = new ChessBoardNavigator(chessGame.getChessBoard());
        String firstPlayer = chessGame.getPlayerForWhite().getUsername();
        String secondPlayer = chessGame.getPlayerForBlack().getUsername();

        System.out.println(navigator.prettyToString());

        chessGame.makeMovement(firstPlayer, h5, g6, null);
        System.out.println(navigator.prettyToString());

        chessGame.returnMovement(firstPlayer);
        chessGame.returnMovement(secondPlayer);
        System.out.println(navigator.prettyToString());

        chessGame.makeMovement(firstPlayer, h5, g6, null);
        System.out.println(navigator.prettyToString());

        chessGame.returnMovement(firstPlayer);
        chessGame.returnMovement(secondPlayer);
        System.out.println(navigator.prettyToString());

        chessGame.makeMovement(firstPlayer, h5, g6, null);
        System.out.println(navigator.prettyToString());
    }

    @Test
    void enPassaun() {
        ChessGame chessGame = chessGameSupplier("3b4/2p5/6b1/2pk2p1/1pP1N1P1/pP3P2/P7/3KB3 b - c3 0 32").get();
        ChessBoardNavigator navigator = new ChessBoardNavigator(chessGame.getChessBoard());
        String firstPlayer = chessGame.getPlayerForWhite().getUsername();
        String secondPlayer = chessGame.getPlayerForBlack().getUsername();

        System.out.println(navigator.prettyToString());

        chessGame.makeMovement(secondPlayer, b4, c3, null);
        System.out.println(navigator.prettyToString());

        chessGame.returnMovement(firstPlayer);
        chessGame.returnMovement(secondPlayer);
        System.out.println(navigator.prettyToString());

        chessGame.makeMovement(secondPlayer, b4, c3, null);
        System.out.println(navigator.prettyToString());
    }
}
