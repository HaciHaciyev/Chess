package core.project.chess.domain.Perft;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.PerftUtil;

import java.util.List;

@Disabled("For technical reasons. Need to be executed separately.")
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
        String fen =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        System.out.println("Perft good position: " + fen);
        our_board = ChessBoard.pureChessFromPosition(fen);

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    void perftPromotionPosition() {
        int depth = 6;
        String fen = "n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1";
        System.out.println("Perft promotion position: " + fen);
        our_board = ChessBoard.pureChessFromPosition(fen);

        long nodes = perft(depth);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    void perftCustomPositionsLite() {
        List<PerftTask> perftTasks = PerftUtil.read_perft_tasks();
        long total_nodes = 0;
        int processed_fen = 0;

        for (int task_idx = 0; task_idx < perftTasks.size(); task_idx++) {
            PerftTask task = perftTasks.get(task_idx);

            String fen = task.fen();
            System.out.println(
                "Perft custom position #%s: %s".formatted(task_idx, fen)
            );

            for (int j = 0; j < task.values().length - 2; j++) {
                int depth = j + 1;
                our_board = ChessBoard.pureChessFromPosition(fen);

                long nodes = perft(depth);
                total_nodes += nodes;
                System.out.println("\t Depth: " + depth + " \tNodes: " + nodes);
                Assertions.assertThat(nodes).isEqualTo(task.values()[j]);
            }

            processed_fen++;
        }

        System.out.println("Processed %s fens".formatted(processed_fen));
        System.out.println("Total %s nodes".formatted(total_nodes));
    }

    @Test
    void perftCustomPositionsMid() {
        List<PerftTask> perftTasks = PerftUtil.read_perft_tasks();
        long total_nodes = 0;
        int processed_fen = 0;

        for (int task_idx = 0; task_idx < perftTasks.size(); task_idx++) {
            PerftTask task = perftTasks.get(task_idx);

            String fen = task.fen();
            System.out.println(
                "Perft custom position #%s: %s".formatted(task_idx, fen)
            );

            for (int j = 0; j < task.values().length - 1; j++) {
                int depth = j + 1;
                our_board = ChessBoard.pureChessFromPosition(fen);

                long nodes = perft(depth);
                total_nodes += nodes;
                System.out.println("\t Depth: " + depth + " \tNodes: " + nodes);
                Assertions.assertThat(nodes).isEqualTo(task.values()[j]);
            }

            processed_fen++;
        }

        System.out.println("Processed %s fens".formatted(processed_fen));
        System.out.println("Total %s nodes".formatted(total_nodes));
    }

    @Test
    void perftCustomPositionsVerbose() {
        List<PerftTask> perftTasks = PerftUtil.read_perft_tasks();
        long total_nodes = 0;
        int processed_fen = 0;

        for (int task_idx = 0; task_idx < perftTasks.size(); task_idx++) {
            PerftTask task = perftTasks.get(task_idx);

            String fen = task.fen();
            System.out.println(
                "Perft custom position #%s: %s".formatted(task_idx, fen)
            );

            for (int j = 0; j < task.values().length; j++) {
                int depth = j + 1;
                our_board = ChessBoard.pureChessFromPosition(fen);

                long nodes = perft(depth);
                total_nodes += nodes;
                System.out.println("\t Depth: " + depth + " \tNodes: " + nodes);
                Assertions.assertThat(nodes).isEqualTo(task.values()[j]);
            }

            processed_fen++;
        }

        System.out.println("Processed %s fens".formatted(processed_fen));
        System.out.println("Total %s nodes".formatted(total_nodes));
    }

    long perft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<core.project.chess.domain.chess.value_objects.Move> legal_moves =
            null;

        try {
            legal_moves = our_board.generateAllValidMoves();
        } catch (Exception e) {
            System.out.printf(
                "Could not generate moves for position: %s | current depth: %s%n",
                our_board.toString(),
                depth
            );
            throw e;
        }

        for (var move : legal_moves) {
            Coordinate from = move.from();
            Coordinate to = move.to();
            Piece inCaseOfPromotion = move.promotion();

            try {
                our_board.doMove(from, to, inCaseOfPromotion);
            } catch (Exception e) {
                System.out.printf(
                    "Error making move: %s | position: %s | depth: %s%n",
                    move,
                    our_board.toString(),
                    depth
                );
                throw e;
            }

            long newNodes = perft(depth - 1);
            nodes += newNodes;

            our_board.undoMove();
        }

        return nodes;
    }
}
