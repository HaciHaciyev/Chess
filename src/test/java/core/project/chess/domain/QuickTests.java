package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Queen;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.Move;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled("Just utility")
class QuickTests {
    private final ChessBoard chessBoard = ChessBoard.pureChessFromPosition("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1");
    private final ToStringUtils navigator = new ToStringUtils(chessBoard);

    @Test
    void test() {
        //  PGN: g2xf1=Q+, Ke2-e3, Kd7-c6, Ke3-d4
        Log.info(navigator.prettyToString());

        chessBoard.doMove(Coordinate.g2, Coordinate.f1, Queen.of(Color.BLACK));
        Log.info(navigator.prettyToString());

        chessBoard.doMove(Coordinate.e2, Coordinate.e3);
        Log.info(navigator.prettyToString());

        chessBoard.doMove(Coordinate.d7, Coordinate.c6);
        Log.info(navigator.prettyToString());

        chessBoard.doMove(Coordinate.e3, Coordinate.d4);
        Log.info(navigator.prettyToString());

        List<Move> moves = navigator.board().generateAllValidMoves();
        assertNotContains(moves, new Move(Coordinate.c6, Coordinate.d5, null));
        assertNotContains(moves, new Move(Coordinate.c6, Coordinate.c5, null));
        Log.info(moves);
    }

    @Test
    void moveGenerationTest() {
        ChessBoard chessBoard = ChessBoard.fromPosition("r3k2r/p1ppqpb1/b3pnp1/3PN3/1p2P3/2N2Q1p/PnPB1PPP/R2B1K1R w kq - 0 3");
        ToStringUtils toStringUtils = new ToStringUtils(chessBoard);
        logBoard(toStringUtils);

        List<Move> allValidMoves = chessBoard.generateAllValidMoves();
        System.out.println(allValidMoves);
        assertContains(allValidMoves, new Move(Coordinate.c3, Coordinate.b5, null));

        chessBoard.doMove(Coordinate.c3, Coordinate.b5);
        logBoard(toStringUtils);

        chessBoard.undoMove();
        logBoard(toStringUtils);

        List<Move> allValidMoves1 = chessBoard.generateAllValidMoves();
        System.out.println(allValidMoves1);
        assertContains(allValidMoves1, new Move(Coordinate.c3, Coordinate.b5, null));

        chessBoard.doMove(Coordinate.c3, Coordinate.b5);
        logBoard(toStringUtils);

        chessBoard.undoMove();
        logBoard(toStringUtils);

        chessBoard.doMove(Coordinate.c3, Coordinate.e2);
        logBoard(toStringUtils);
    }

    private static void logBoard(ToStringUtils toStringUtils) {
        System.out.println(toStringUtils.prettyToString());
    }

    private static void assertContains(List<Move> allValidMoves, Object o) {
        if (!allValidMoves.contains(o))
            throw new AssertionError("Do not contains required data.");
    }

    private static void assertNotContains(List<Move> allValidMoves, Object o) {
        if (allValidMoves.contains(o))
            throw new AssertionError("Contains invalid data: {%s}".formatted(o.toString()));
    }
}