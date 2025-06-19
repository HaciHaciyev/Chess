package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.Move;
import core.project.chess.domain.commons.enumerations.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomChessTests {

    @Test
    void revertPromotion() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ToStringUtils boardStringUtils = new ToStringUtils(chessBoard);

        //1.
        chessBoard.doMove( e2, e4, null);
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( e7, e5, null);
        System.out.println(boardStringUtils.prettyToString());

        //2.
        chessBoard.doMove( f2, f4, null);
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( e5, f4, null);
        System.out.println(boardStringUtils.prettyToString());

        //3.
        chessBoard.doMove( g2, g3, null);
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( f4, g3, null);
        System.out.println(boardStringUtils.prettyToString());

        //4.
        chessBoard.doMove( g1, f3, null);
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( g3, h2, null);
        System.out.println(boardStringUtils.prettyToString());

        //5.
        chessBoard.doMove( f3, g1, null);
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( h2, g1, Queen.of(Color.BLACK));
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.undoMove();
        System.out.println(boardStringUtils.prettyToString());

        chessBoard.doMove( d8, h4, Queen.of(Color.BLACK));
        System.out.println(boardStringUtils.prettyToString());
    }

    @Test
    void enPassaunOnCheck() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ToStringUtils navigator = new ToStringUtils(chessBoard);

        System.out.println(navigator.prettyToString());

        chessBoard.doMove( h5, g6, null);
        System.out.println(navigator.prettyToString());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());

        chessBoard.doMove( h5, g6, null);
        System.out.println(navigator.prettyToString());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());

        chessBoard.doMove( h5, g6, null);
        System.out.println(navigator.prettyToString());
    }

    @Test
    void enPassaun() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ToStringUtils navigator = new ToStringUtils(chessBoard);

        System.out.println(navigator.prettyToString());

        chessBoard.doMove(b4, c3, null);
        System.out.println(navigator.prettyToString());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());

        chessBoard.doMove( b4, c3, null);
        System.out.println(navigator.prettyToString());
    }

    @Test
    @DisplayName("undo move")
    void undoMove() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ToStringUtils navigator = new ToStringUtils(chessBoard);

        chessBoard.doMove(e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove(e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove( a7, a6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove(e4, e5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove( d7, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove(e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove( d7, d5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.doMove(e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        chessBoard.undoMove();
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());
    }

    @Test
    @DisplayName("Chess chessBoard end by pat in 10 move.")
    void testChessGameEndByPat() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        // 1.
        chessBoard.doMove( e2, e3, null);

        chessBoard.doMove( a7, a5, null);

        // 2.
        chessBoard.doMove( d1, h5, null);

        chessBoard.doMove( a8, a6, null);

        // 3.
        chessBoard.doMove( h5, a5, null);

        chessBoard.doMove( h7, h5, null);

        // 4.
        chessBoard.doMove( a5, c7, null);

        chessBoard.doMove( a6, h6, null);

        // 5.
        chessBoard.doMove( h2, h4, null);

        chessBoard.doMove( f7, f6, null);

        // 6.
        chessBoard.doMove( c7, d7, null);

        chessBoard.doMove( e8, f7, null);

        // 7.
        chessBoard.doMove( d7, b7, null);

        chessBoard.doMove( d8, d3, null);

        // 8.
        chessBoard.doMove( b7, b8, null);

        chessBoard.doMove( d3, h7, null);

        // 9.
        chessBoard.doMove( b8, c8, null);

        chessBoard.doMove( f7, g6, null);

        // 10. STALEMATE
        chessBoard.doMove( c8, e6, null);
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
    @DisplayName("Custom chessBoard")
    void chessBoard() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        //1.
        chessBoard.doMove( e2, e4, null);

        chessBoard.doMove( e7, e5, null);

        //2.
        chessBoard.doMove( g1, f3, null);

        chessBoard.doMove( b8, c6, null);

        //3.
        chessBoard.doMove( b1, c3, null);

        chessBoard.doMove( g8, f6, null);

        //4.
        chessBoard.doMove( d2, d3, null);

        chessBoard.doMove( f8, c5, null);

        //5.
        chessBoard.doMove( c1, g5, null);

        chessBoard.doMove( h7, h6, null);

        //6.
        chessBoard.doMove( g5, f6, null);

        chessBoard.doMove( d8, f6, null);

        //7.
        chessBoard.doMove( a2, a3, null);

        chessBoard.doMove( d7, d6, null);

        //8.
        chessBoard.doMove( c3, d5, null);

        chessBoard.doMove( f6, d8, null);

        //9.
        chessBoard.doMove( c2, c3, null);

        chessBoard.doMove( c8, e6, null);

        //10.
        chessBoard.doMove( d1, a4, null);

        chessBoard.doMove( d8, d7, null);

        //11.
        chessBoard.doMove( d5, e3, null);

        chessBoard.doMove( c5, e3, null);

        //12.
        chessBoard.doMove( f2, e3, null);

        chessBoard.doMove( e8, g8, null);

        //13.
        chessBoard.doMove( d3, d4, null);

        chessBoard.doMove( e6, g4, null);

        //14.
        chessBoard.doMove( f3, d2, null);

        chessBoard.doMove( d7, e7, null);

        //15.
        chessBoard.doMove( d4, e5, null);

        chessBoard.doMove( d6, e5, null);

        //16.
        chessBoard.doMove( d2, c4, null);

        chessBoard.doMove( e7, g5, null);

        //17.
        chessBoard.doMove( f1, d3, null);

        chessBoard.doMove( g4, e6, null);

        //18.
        chessBoard.doMove( g2, g3, null);

        chessBoard.doMove( e6, c4, null);

        //19.
        chessBoard.doMove( d3, c4, null);

        chessBoard.doMove( g5, e3, null);

        //20. fixed
        chessBoard.doMove( c4, e2, null);

        chessBoard.doMove( a7, a6, null);

        //21.
        chessBoard.doMove( h1, f1, null);

        chessBoard.doMove( b7, b5, null);

        //22.
        chessBoard.doMove( a4, c2, null);

        chessBoard.doMove( a6, a5, null);

        //23.
        chessBoard.doMove( c3, c4, null);

        chessBoard.doMove( b5, b4, null);

        //24.
        chessBoard.doMove( a3, b4, null);

        chessBoard.doMove( c6, b4, null);

        //25.
        chessBoard.doMove( c2, b1, null);

        chessBoard.doMove( f8, d8, null);

        //26.
        chessBoard.doMove( f1, f5, null);

        chessBoard.doMove( f7, f6, null);

        //27.
        chessBoard.doMove( a1, a3, null);

        chessBoard.doMove( e3, d2, null);

        //28.
        chessBoard.doMove( e1, f1, null);

        chessBoard.doMove( d2, c2, null);

        //29.
        chessBoard.doMove( a3, a1, null);

        chessBoard.doMove( d8, d4, null);

        //30.
        chessBoard.doMove( e2, f3, null);

        chessBoard.doMove( c2, c4, null);

        //31.
        chessBoard.doMove( f1, g2, null);

        chessBoard.doMove( d4, d2, null);

        //32.
        chessBoard.doMove( g2, g1, null);

        chessBoard.doMove( g8, f7, null);

        //33.
        chessBoard.doMove( b1, f1, null);

        chessBoard.doMove( c4, f1, null);

        //34.
        chessBoard.doMove( g1, f1, null);

        chessBoard.doMove( g7, g6, null);

        //35.
        chessBoard.doMove( f1, e1, null);

        chessBoard.doMove( d2, b2, null);

        //36.
        chessBoard.doMove( f3, h5, null);

        chessBoard.doMove( b4, c2, null);

        //37.
        chessBoard.doMove( e1, f2, null);

        chessBoard.doMove( c2, a1, null);

        //38.
        chessBoard.doMove( f2, f3, null);

        chessBoard.doMove( a8, d8, null);

        //39.
        chessBoard.doMove( f3, g4, null);

        chessBoard.doMove( c7, c5, null);

        //40.
        chessBoard.doMove( h2, h4, null);

        chessBoard.doMove( c5, c4, null);

        //41.
        chessBoard.doMove( g4, f3, null);

        chessBoard.doMove( c4, c3, null);

        //42. checkmate
        chessBoard.doMove( g3, g4, null);

        chessBoard.doMove( d8, d3, null);
    }
}
