package core.project.chess.domain.Perft;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Disabled;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import org.junit.jupiter.api.Test;
import testUtils.PerftUtil;

public class ClearPerftTest {
    private ChessBoard our_board;

    @Test
    void perftStandartPosition() {
        System.out.println("Perft standart position");
        int depth = 6;
        our_board = ChessBoard.pureChess();

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);

        switch (depth) {
            case 1 -> PerftUtil.assertPerftDepth1(nodes);
            case 2 -> PerftUtil.assertPerftDepth2(nodes);
            case 3 -> PerftUtil.assertPerftDepth3(nodes);
            case 4 -> PerftUtil.assertPerftDepth4(nodes);
            case 5 -> PerftUtil.assertPerftDepth5(nodes);
            case 6 -> PerftUtil.assertPerftDepth6(nodes);
            case 7 -> PerftUtil.assertPerftDepth7(nodes);
            case 8 -> PerftUtil.assertPerftDepth8(nodes);
        }

    }

    @Test
    void perftGoodPosition() {
        int depth = 5;
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        System.out.println("Perft good position: " + fen);
        our_board = ChessBoard.pureChessFromPosition(fen);

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    void perftPromotionPosition() {
        int depth = 5;
        String fen = "n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1";
        System.out.println("Perft promotion position: " + fen);
        our_board = ChessBoard.pureChessFromPosition(fen);

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    @Disabled
    void perftCustomPosition() {
        int depth = 0;
        String fen = "";
        System.out.println("Perft custom position: " + fen);
        our_board = ChessBoard.pureChessFromPosition(fen);

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);
    }

    long perft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<core.project.chess.domain.chess.value_objects.Move> legal_moves = null;

        try {
            legal_moves = our_board.generateAllValidMoves();
        } catch (Exception e) {
            System.out.printf("Could not generate moves for position: %s | current depth: %s%n",
                    our_board.actualRepresentationOfChessBoard(), depth);
            throw e;
        }

        for (var move : legal_moves) {
            Coordinate from = move.from();
            Coordinate to = move.to();
            Piece inCaseOfPromotion = move.promotion();

            try {
                our_board.doMove(from, to, inCaseOfPromotion);
            } catch (Exception e) {
                System.out.printf("Error making move: %s | position: %s | depth: %s%n", move,
                        our_board.actualRepresentationOfChessBoard(), depth);
                throw e;
            }

            long newNodes = perft(depth - 1);
            nodes += newNodes;

            our_board.undoMove();
        }

        return nodes;
    }
}
