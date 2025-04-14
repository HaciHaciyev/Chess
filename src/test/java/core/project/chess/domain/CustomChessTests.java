package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.Move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static core.project.chess.domain.entities.ChessGameTest.chessGameSupplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomChessTests {

    @Test
    void revertPromotion() {
        ChessGame game = chessGameSupplier().get();
        ToStringUtils navigator = new ToStringUtils(game.getChessBoard());

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
        ToStringUtils navigator = new ToStringUtils(chessGame.getChessBoard());
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
        ToStringUtils navigator = new ToStringUtils(chessGame.getChessBoard());
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

    @Test
    @DisplayName("undo move")
    void undoMove() {
        ChessGame game = chessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername();
        String black = game.getPlayerForBlack().getUsername();

        ToStringUtils navigator = new ToStringUtils(game.getChessBoard());

        game.makeMovement(white, e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, a7, a6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e4, e5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, d7, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, d7, d5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());
    }

    @Test
    @DisplayName("Chess game end by pat in 10 move.")
    void testChessGameEndByPat() {
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername();

        // 1.
        chessGame.makeMovement(firstPlayerUsername, e2, e3, null);

        chessGame.makeMovement(secondPlayerUsername, a7, a5, null);

        // 2.
        chessGame.makeMovement(firstPlayerUsername, d1, h5, null);

        chessGame.makeMovement(secondPlayerUsername, a8, a6, null);

        // 3.
        chessGame.makeMovement(firstPlayerUsername, h5, a5, null);

        chessGame.makeMovement(secondPlayerUsername, h7, h5, null);

        // 4.
        chessGame.makeMovement(firstPlayerUsername, a5, c7, null);

        chessGame.makeMovement(secondPlayerUsername, a6, h6, null);

        // 5.
        chessGame.makeMovement(firstPlayerUsername, h2, h4, null);

        chessGame.makeMovement(secondPlayerUsername, f7, f6, null);

        // 6.
        chessGame.makeMovement(firstPlayerUsername, c7, d7, null);

        chessGame.makeMovement(secondPlayerUsername, e8, f7, null);

        // 7.
        chessGame.makeMovement(firstPlayerUsername, d7, b7, null);

        chessGame.makeMovement(secondPlayerUsername, d8, d3, null);

        // 8.
        chessGame.makeMovement(firstPlayerUsername, b7, b8, null);

        chessGame.makeMovement(secondPlayerUsername, d3, h7, null);

        // 9.
        chessGame.makeMovement(firstPlayerUsername, b8, c8, null);

        chessGame.makeMovement(secondPlayerUsername, f7, g6, null);

        // 10. STALEMATE
        chessGame.makeMovement(firstPlayerUsername, c8, e6, null);

        assertEquals(GameResult.DRAW, chessGame.gameResult().orElseThrow());
    }

    @Test
    void isPathClearTest() {
        ChessBoard chessBoard = ChessBoard.pureChess();
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(c1, e3));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(c1, d2));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(e1, e5));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(d1, c1));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(d1, d2));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(d1, d5));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(d1, b3));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(d1, c2));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(e1, e2));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(e1, d1));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(a1, b1));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(a1, h1));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(a1, a2));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(a1, a5));

    }

    @Test
    void moveGenerationTests() {
        ChessBoard chessBoard = ChessBoard.pureChess();
        System.out.println("All valid moves (default position) " + chessBoard.generateAllValidMoves().toString());
        List<Move> whitePawnMoves = Pawn.of(Color.WHITE).allValidMoves(chessBoard);
        System.out.println("White pawn moves (default position) " + whitePawnMoves);
        System.out.println("White knight moves (default position) " + Knight.of(Color.WHITE).allValidMoves(chessBoard));
        System.out.println("White bishop moves (default position) " + Bishop.of(Color.WHITE).allValidMoves(chessBoard));
        System.out.println("White rook moves (default position) " + Rook.of(Color.WHITE).allValidMoves(chessBoard));
        System.out.println("White queen moves (default position) " + Queen.of(Color.WHITE).allValidMoves(chessBoard));
        System.out.println("White king moves (default position) " + King.of(Color.WHITE).allValidMoves(chessBoard));

        System.out.println();
        chessBoard.doMove(Coordinate.a2, Coordinate.a3);
        System.out.println("All valid moves (default position + a2-a3) " + chessBoard.generateAllValidMoves().toString());
        List<Move> blackPawnMoves = Pawn.of(Color.BLACK).allValidMoves(chessBoard);
        System.out.println("Black pawn moves (default position + a2-a3) " + blackPawnMoves);
        System.out.println("Black knight moves (default position + a2-a3) " + Knight.of(Color.BLACK).allValidMoves(chessBoard));
        System.out.println("Black bishop moves (default position + a2-a3) " + Bishop.of(Color.BLACK).allValidMoves(chessBoard));
        System.out.println("Black rook moves (default position + a2-a3) " + Rook.of(Color.BLACK).allValidMoves(chessBoard));
        System.out.println("Black queen moves (default position + a2-a3) " + Queen.of(Color.BLACK).allValidMoves(chessBoard));
        System.out.println("Black king moves (default position + a2-a3) " + King.of(Color.BLACK).allValidMoves(chessBoard));

        chessBoard.doMove(b7, b5);
        chessBoard.doMove(a3, a4);
        System.out.println();
        System.out.println(Pawn.of(Color.BLACK).allValidMoves(chessBoard));
        chessBoard.doMove(b5, b4);

        System.out.println();
        System.out.println(Pawn.of(Color.WHITE).allValidMoves(chessBoard));
        assertThrows(IllegalArgumentException.class, () -> chessBoard.doMove(b2, b4));
        chessBoard.doMove(b2, b3);

        ToStringUtils toStringUtils = new ToStringUtils(chessBoard);

        System.out.println(toStringUtils.prettyToString());
        chessBoard.doMove(a7, a5);
        chessBoard.doMove(c2, c4);

        System.out.println();
        System.out.println(Pawn.of(Color.BLACK).allValidMoves(chessBoard));
        chessBoard.doMove(b4, c3);

        System.out.println(toStringUtils.prettyToString());
        chessBoard.doMove(b3, b4);

        System.out.println(toStringUtils.prettyToString());
        chessBoard.doMove(c3, d2);

        System.out.println(toStringUtils.prettyToString());
        chessBoard.undoMove();

        System.out.println(toStringUtils.prettyToString());

        chessBoard.doMove(c3, c2);
        System.out.println(toStringUtils.prettyToString());

        chessBoard.doMove(b4, b5);
        System.out.println(toStringUtils.prettyToString());

        System.out.println();
        System.out.println(Pawn.of(Color.BLACK).allValidMoves(chessBoard));
        chessBoard.doMove(c2, b1, Queen.of(Color.BLACK));
        System.out.println(toStringUtils.prettyToString());
    }

    @Test
    @DisplayName("Custom game")
    void game() {
        ChessGame game = chessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername();
        String blackPlayer = game.getPlayerForBlack().getUsername();

        //1.
        game.makeMovement(whitePlayer, e2, e4, null);

        game.makeMovement(blackPlayer, e7, e5, null);

        //2.
        game.makeMovement(whitePlayer, g1, f3, null);

        game.makeMovement(blackPlayer, b8, c6, null);

        //3.
        game.makeMovement(whitePlayer, b1, c3, null);

        game.makeMovement(blackPlayer, g8, f6, null);

        //4.
        game.makeMovement(whitePlayer, d2, d3, null);

        game.makeMovement(blackPlayer, f8, c5, null);

        //5.
        game.makeMovement(whitePlayer, c1, g5, null);

        game.makeMovement(blackPlayer, h7, h6, null);

        //6.
        game.makeMovement(whitePlayer, g5, f6, null);

        game.makeMovement(blackPlayer, d8, f6, null);

        //7.
        game.makeMovement(whitePlayer, a2, a3, null);

        game.makeMovement(blackPlayer, d7, d6, null);

        //8.
        game.makeMovement(whitePlayer, c3, d5, null);

        game.makeMovement(blackPlayer, f6, d8, null);

        //9.
        game.makeMovement(whitePlayer, c2, c3, null);

        game.makeMovement(blackPlayer, c8, e6, null);

        //10.
        game.makeMovement(whitePlayer, d1, a4, null);

        game.makeMovement(blackPlayer, d8, d7, null);

        //11.
        game.makeMovement(whitePlayer, d5, e3, null);

        game.makeMovement(blackPlayer, c5, e3, null);

        //12.
        game.makeMovement(whitePlayer, f2, e3, null);

        game.makeMovement(blackPlayer, e8, g8, null);

        //13.
        game.makeMovement(whitePlayer, d3, d4, null);

        game.makeMovement(blackPlayer, e6, g4, null);

        //14.
        game.makeMovement(whitePlayer, f3, d2, null);

        game.makeMovement(blackPlayer, d7, e7, null);

        //15.
        game.makeMovement(whitePlayer, d4, e5, null);

        game.makeMovement(blackPlayer, d6, e5, null);

        //16.
        game.makeMovement(whitePlayer, d2, c4, null);

        game.makeMovement(blackPlayer, e7, g5, null);

        //17.
        game.makeMovement(whitePlayer, f1, d3, null);

        game.makeMovement(blackPlayer, g4, e6, null);

        //18.
        game.makeMovement(whitePlayer, g2, g3, null);

        game.makeMovement(blackPlayer, e6, c4, null);

        //19.
        game.makeMovement(whitePlayer, d3, c4, null);

        game.makeMovement(blackPlayer, g5, e3, null);

        //20. fixed
        game.makeMovement(whitePlayer, c4, e2, null);

        game.makeMovement(blackPlayer, a7, a6, null);

        //21.
        game.makeMovement(whitePlayer, h1, f1, null);

        game.makeMovement(blackPlayer, b7, b5, null);

        //22.
        game.makeMovement(whitePlayer, a4, c2, null);

        game.makeMovement(blackPlayer, a6, a5, null);

        //23.
        game.makeMovement(whitePlayer, c3, c4, null);

        game.makeMovement(blackPlayer, b5, b4, null);

        //24.
        game.makeMovement(whitePlayer, a3, b4, null);

        game.makeMovement(blackPlayer, c6, b4, null);

        //25.
        game.makeMovement(whitePlayer, c2, b1, null);

        game.makeMovement(blackPlayer, f8, d8, null);

        //26.
        game.makeMovement(whitePlayer, f1, f5, null);

        game.makeMovement(blackPlayer, f7, f6, null);

        //27.
        game.makeMovement(whitePlayer, a1, a3, null);

        game.makeMovement(blackPlayer, e3, d2, null);

        //28.
        game.makeMovement(whitePlayer, e1, f1, null);

        game.makeMovement(blackPlayer, d2, c2, null);

        //29.
        game.makeMovement(whitePlayer, a3, a1, null);

        game.makeMovement(blackPlayer, d8, d4, null);

        //30.
        game.makeMovement(whitePlayer, e2, f3, null);

        game.makeMovement(blackPlayer, c2, c4, null);

        //31.
        game.makeMovement(whitePlayer, f1, g2, null);

        game.makeMovement(blackPlayer, d4, d2, null);

        //32.
        game.makeMovement(whitePlayer, g2, g1, null);

        game.makeMovement(blackPlayer, g8, f7, null);

        //33.
        game.makeMovement(whitePlayer, b1, f1, null);

        game.makeMovement(blackPlayer, c4, f1, null);

        //34.
        game.makeMovement(whitePlayer, g1, f1, null);

        game.makeMovement(blackPlayer, g7, g6, null);

        //35.
        game.makeMovement(whitePlayer, f1, e1, null);

        game.makeMovement(blackPlayer, d2, b2, null);

        //36.
        game.makeMovement(whitePlayer, f3, h5, null);

        game.makeMovement(blackPlayer, b4, c2, null);

        //37.
        game.makeMovement(whitePlayer, e1, f2, null);

        game.makeMovement(blackPlayer, c2, a1, null);

        //38.
        game.makeMovement(whitePlayer, f2, f3, null);

        game.makeMovement(blackPlayer, a8, d8, null);

        //39.
        game.makeMovement(whitePlayer, f3, g4, null);

        game.makeMovement(blackPlayer, c7, c5, null);

        //40.
        game.makeMovement(whitePlayer, h2, h4, null);

        game.makeMovement(blackPlayer, c5, c4, null);

        //41.
        game.makeMovement(whitePlayer, g4, f3, null);

        game.makeMovement(blackPlayer, c4, c3, null);

        //42. checkmate
        game.makeMovement(whitePlayer, g3, g4, null);

        game.makeMovement(blackPlayer, d8, d3, null);
    }

    public void chessGameLoad() {
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername();

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, e7, e5, null)
        );

        // INVALID. Valid players turn but invalid pieces usage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e7, e5, null)
        );

        // INVALID. Piece not exists.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e3, e4, null)
        );

        // INVALID. Invalid Pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e2, e5, null)
        );

        // INVALID. Invalid Pawn move, can`t capture void.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e2, d3, null)
        );

        // VALID. First valid move, pawn passage.
        chessGame.makeMovement(firstPlayerUsername, e2, e4, null);

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c8, f5, null)
        );

        // INVALID. Valid players turn, but invalid figures turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c1, f4, null)
        );

        // INVALID. Bishop can`t move when path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, c8, f5, null)
        );

        // VALID. Valid pawn passage.
        chessGame.makeMovement(secondPlayerUsername, e7, e5, null);

        // INVALID. King can`t long castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e1, c1, null)
        );

        // INVALID. King can`t short castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e1, g1, null)
        );

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, a1, a5, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b1, b3, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(firstPlayerUsername, b1, c3, null);

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, a8, a4, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b8, b6, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(secondPlayerUsername, g8, f6, null);

        // INVALID. Invalid pawn passage, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, c3, null)
        );

        // INVALID. Invalid pawn move, end field is not.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, c3, null)
        );

        // INVALID. Invalid pawn capture status, nothing to capture and it is not a capture on passage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, d3, null)
        );

        // INVALID. Invalid pawn move, can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e4, e3, null)
        );

        // INVALID. Invalid pawn move, can`t move diagonal-back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e4, f3, null)
        );

        // INVALID. Invalid Queen move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, d1, e3, null)
        );

        // VALID. Knight move.
        chessGame.makeMovement(firstPlayerUsername, g1, f3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, b8, c6, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, f1, b5, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, a7, a6, null);

        // INVALID. Invalid Bishop move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b5, d5, null)
        );

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, d2, d3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, d7, d6, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(firstPlayerUsername, b5, c6, null);

        // INVALID. Valid Bishop move but it`s not safety for King.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, f8, e7, null)
        );

        // VALID. Pawn captures enemy bishop threatening King.
        chessGame.makeMovement(secondPlayerUsername, b7, c6, null);

        // VALID. Short castle.
        chessGame.makeMovement(firstPlayerUsername, e1, g1, null);

        // INVALID. Invalid rook move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, a8, b6, null)
        );

        // INVALID. Invalid pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, c6, c4, null)
        );

        // INVALID. Valid Knight move but end field occupied by same color piece.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, f6, h7, null)
        );

        // INVALID. Invalid King move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, e8, e6, null)
        );

        // INVALID. Invalid pawn movement, pawn can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.g7, Coordinate.g8, null)
        );

        // INVALID. Invalid Knight move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.f6, Coordinate.f5, null)
        );

        // INVALID. Invalid diagonal pawn movement distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.d3, Coordinate.e4, null)
        );

        // VALID. Bishop move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.c8, Coordinate.g4, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.h2, Coordinate.h3, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.g4, Coordinate.f3, null);

        // VALID. Queen capture Bishop.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.d1, Coordinate.f3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.f6, Coordinate.d7, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.c1, Coordinate.e3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.f7, Coordinate.f6, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.a2, Coordinate.a3, null);

        // VALID, Rook move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.a8, Coordinate.b8, null);
    }

    @Test
    void temp() {
        ChessBoard chessBoard = ChessBoard.fromPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println("WHITE PAWNS: " + chessBoard.bitboard(Pawn.of(Color.WHITE)));
        System.out.println("BLACK PAWNS: " + chessBoard.bitboard(Pawn.of(Color.BLACK)));
        System.out.println("WHITE KNIGHTS: " + chessBoard.bitboard(Knight.of(Color.WHITE)));
        System.out.println("BLACK KNIGHTS: " + chessBoard.bitboard(Knight.of(Color.BLACK)));
        System.out.println("WHITE BISHOPS: " + chessBoard.bitboard(Bishop.of(Color.WHITE)));
        System.out.println("BLACK BISHOPS: " + chessBoard.bitboard(Bishop.of(Color.BLACK)));
        System.out.println("WHITE ROOKS: " + chessBoard.bitboard(Rook.of(Color.WHITE)));
        System.out.println("BLACK ROOKS: " + chessBoard.bitboard(Rook.of(Color.BLACK)));
        System.out.println("WHITE QUEENS: " + chessBoard.bitboard(Queen.of(Color.WHITE)));
        System.out.println("BLACK QUEENS: " + chessBoard.bitboard(Queen.of(Color.BLACK)));
        System.out.println("WHITE KING: " + chessBoard.bitboard(King.of(Color.WHITE)));
        System.out.println("BLACK KING: " + chessBoard.bitboard(King.of(Color.BLACK)));

        System.out.println();
        System.out.println("WHITES: " + chessBoard.whitePieces());
        System.out.println("BLACKS: " + chessBoard.blackPieces());
    }
}
