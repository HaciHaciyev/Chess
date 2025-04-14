package core.project.chess.domain;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.PerftUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("For separate run.")
class ChessPerft {
    public static int DEPTH = 6;

    private ChessBoard our_board;
    private Board their_board;

    private PerftValues perftValues;
    private PerftValues secondPerftValues;

    @Test
    void moveGenerations() {
        our_board = ChessBoard.pureChess();
        their_board = new Board();

        long start = 0L;
        long end = 0L;

        start = System.nanoTime();
        for (int i = 0; i < 35_000_000; i++) {
            our_board.generateAllValidMoves();
        }
        our_board.doMove(Coordinate.e2, Coordinate.e4);
        for (int i = 0; i < 36_000_000; i++) {
            our_board.generateAllValidMoves();
        }
        end = System.nanoTime();
        System.out.println("Our moves generation time   : " + (end - start) / 1_000_000.0 + " ms");

        start = System.nanoTime();
        for (int i = 0; i < 35_000_000; i++) {
            their_board.legalMoves();
        }
        their_board.doMove(new Move(Square.E2, Square.E4));
        for (int i = 0; i < 36_000_000; i++) {
            their_board.legalMoves();
        }

        end = System.nanoTime();
        System.out.println("Their moves generation time : " + (end - start) / 1_000_000.0 + " ms");
    }

    @Test
    void customPositions() {
        {
            DEPTH = 5;
            perftValues = PerftValues.newInstance();
            secondPerftValues = PerftValues.newInstance();

            String position = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
            System.out.println("Position #1: " + position);

            our_board = ChessBoard.pureChessFromPosition(position);

            their_board = new Board();
            their_board.loadFromFen(position);

            long nodes_good_position = robustPerft(DEPTH);
            System.out.println("Nodes: " + nodes_good_position);
        }

        {
            DEPTH = 5;
            perftValues = PerftValues.newInstance();
            secondPerftValues = PerftValues.newInstance();

            String position = "n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1";
            System.out.println("Position #2: " + position);

            our_board = ChessBoard.pureChessFromPosition(position);

            their_board = new Board();
            their_board.loadFromFen(position);

            long nodes_good_position = robustPerft(DEPTH);
            System.out.println("Nodes: " + nodes_good_position);
        }
    }

    @Test
    void clearPerft() {
        our_board = ChessBoard.pureChess();

        long nodes = onlyNodesPerft(DEPTH);
        switch (DEPTH) {
            case 1 -> assertPerftDepth1(nodes);
            case 2 -> assertPerftDepth2(nodes);
            case 3 -> assertPerftDepth3(nodes);
            case 4 -> assertPerftDepth4(nodes);
            case 5 -> assertPerftDepth5(nodes);
            case 6 -> assertPerftDepth6(nodes);
            case 7 -> assertPerftDepth7(nodes);
            case 8 -> assertPerftDepth8(nodes);
            case 9 -> assertPerftDepth9(nodes);
            default -> logValues();
        }
    }

    @Test
    void performanceTest() {
        perftValues = PerftValues.newInstance();
        secondPerftValues = PerftValues.newInstance();

        our_board = ChessBoard.pureChess();
        their_board = new Board();
        long v = perft(DEPTH);
        switch (DEPTH) {
            case 1 -> assertPerftDepth1();
            case 2 -> assertPerftDepth2();
            case 3 -> assertPerftDepth3();
            case 4 -> assertPerftDepth4();
            case 5 -> assertPerftDepth5();
            case 6 -> assertPerftDepth6();
            case 7 -> assertPerftDepth7();
            case 8 -> assertPerftDepth8();
            case 9 -> assertPerftDepth9();
            default -> logValues();
        }

        System.out.println("Total nodes -> " + v);
        System.out.println();
        Log.warnf("Performance test executed at depth {%s} but no assertion was performed.", DEPTH);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    // normal perft. Data is collected and measured after every move and compared
    //
    // foreign move generation
    long perft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<Move> their_valid_moves = their_board.legalMoves();

        for (Move move : their_valid_moves) {
            Coordinate from = from(move);
            Coordinate to = to(move);
            Piece inCaseOfPromotion = getInCaseOfPromotion(move);

            their_board.doMove(move);
            our_board.doMove(from, to, inCaseOfPromotion);

            long newNodes = perft(depth - 1);
            nodes += newNodes;
            calculatePerftValues(nodes);
            calculateSecondPerftValues(nodes, move);
            verifyPerft(move, from, to, inCaseOfPromotion);

            their_board.undoMove();
            our_board.undoMove();

            if (depth == DEPTH) {
                System.out.printf("%s -> %s \t|\t %s\n", move, newNodes, our_board.actualRepresentationOfChessBoard());
            }
        }

        return nodes;
    }

    // more informative perft. In addition to normal perft, FEN and generated moves are compared.
    //
    // native and foreign move generation
    long robustPerft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<core.project.chess.domain.chess.value_objects.Move> our_valid_moves = null;
        List<Move> their_valid_moves = their_board.legalMoves();

        try {
            our_valid_moves = our_board.generateAllValidMoves();
        } catch (Exception e) {
            System.out.printf("Could not generate moves for position: %s | current depth: %s%n",
                    our_board.actualRepresentationOfChessBoard(), depth);
            throw e;
        }

        String our_fen = our_board.actualRepresentationOfChessBoard();
        String their_fen = their_board.getFen();

        if (!our_fen.equals(their_fen)) {
            System.out.println("MOVES: " + our_board.listOfAlgebraicNotations());
            System.out.println("OUR FEN: \t" + our_fen);
            System.out.println("THEIR FEN: \t" + their_board.getFen());
            System.out.println();
            PerftUtil.analyze(our_fen, their_fen);
            throw new RuntimeException("FEN mismatch");
        }

        if (our_valid_moves.size() != their_valid_moves.size()) {
            System.out.println();
            System.out.println("OUR FEN: \t" + our_fen);
            System.out.println("THEIR FEN: \t" + their_board.getFen());
            PerftUtil.print_mismatch(our_valid_moves, their_valid_moves);
            throw new RuntimeException("Move generation mismatch");
        }

        for (Move move : their_valid_moves) {
            Coordinate from = from(move);
            Coordinate to = to(move);
            Piece inCaseOfPromotion = getInCaseOfPromotion(move);

            their_board.doMove(move);

            try {
                our_board.doMove(from, to, inCaseOfPromotion);
            } catch (Exception e) {
                System.out.printf("Error making move: %s | position: %s | depth: %s%n", move,
                        our_board.actualRepresentationOfChessBoard(), depth);
                throw e;
            }

            long newNodes = robustPerft(depth - 1);
            nodes += newNodes;
            calculatePerftValues(nodes);
            calculateSecondPerftValues(nodes, move);
            verifyPerft(move, from, to, inCaseOfPromotion);

            their_board.undoMove();
            our_board.undoMove();

            String our_undo_fen = our_board.actualRepresentationOfChessBoard();
            String their_undo_fen = their_board.getFen();

            if (!our_undo_fen.equals(their_undo_fen)) {
                System.out.println("LAST MOVE/UNDO : " + move);
                System.out.println("MOVES: " + our_board.listOfAlgebraicNotations());
                System.out.println("OUR FEN: \t" + our_undo_fen);
                System.out.println("THEIR FEN: \t" + their_undo_fen);
                System.out.println();
                PerftUtil.analyze(our_fen, their_fen);
                throw new RuntimeException("UNDO FEN mismatch");
            }

            if (depth == DEPTH) {
                System.out.printf("%s -> %s \t|\t %s\n", move, newNodes, our_fen);
            }
        }

        return nodes;
    }

    // simple perft. Only nodes are counted
    // 
    // native move generation
    long onlyNodesPerft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<core.project.chess.domain.chess.value_objects.Move> allValidMoves = our_board.generateAllValidMoves();

        for (var move : allValidMoves) {
            Coordinate from = move.from();
            Coordinate to = move.to();
            Piece inCaseOfPromotion = move.promotion();

            our_board.doMove(from, to, inCaseOfPromotion);

            long newNodes = onlyNodesPerft(depth - 1);
            nodes += newNodes;

            our_board.undoMove();
        }

        return nodes;
    }

    private void verifyPerft(Move move, Coordinate from, Coordinate to, Piece inCaseOfPromotion) {
        if (perftValues.verify(secondPerftValues)) {
            return;
        }

        Log.errorf("Perft failed. On move: from - %s, to - %s, inCaseOfPromotion - %s", from, to, inCaseOfPromotion);
        Log.errorf("Our ChessBoard: FEN: %s, PGN: %s", our_board.toString(), our_board.pgn());
        System.out.println();
        their_board.undoMove();
        String[] fen = new String[2];
        fen[0] = their_board.getFen();
        their_board.doMove(move);
        fen[1] = their_board.getFen();
        Log.errorf("External Board: FEN: %s", Arrays.toString(fen));
        System.out.println();

        Log.infof("First nodes: %d", perftValues.nodes);
        Log.infof("Second nodes: %d,", secondPerftValues.nodes);
        System.out.println();

        Log.infof("First captures: %d", perftValues.captures);
        Log.infof("Second captures: %d", secondPerftValues.captures);
        System.out.println();

        Log.infof("First en passaunt: %d", perftValues.capturesOnPassage);
        Log.infof("Second en passaunt: %d", secondPerftValues.capturesOnPassage);
        System.out.println();

        Log.infof("First castles: %d", perftValues.castles);
        Log.infof("Second Castles: %d", secondPerftValues.castles);
        System.out.println();

        Log.infof("First promotions: %d", perftValues.promotions);
        Log.infof("Second promotions: %d", secondPerftValues.promotions);
        System.out.println();

        Log.infof("First checks: %d", perftValues.checks);
        Log.infof("Second checks: %d", secondPerftValues.checks);
        System.out.println();

        Log.infof("First checkMates: %d", perftValues.checkMates);
        Log.infof("Second checkMates: %d", secondPerftValues.checkMates);
        System.out.println();

        throw new IllegalStateException("Invalid perft values.");
    }

    private static @NotNull Coordinate from(Move move) {
        String from = move.getFrom().toString();
        int column = columnToInt(from.charAt(0));
        int row = from.charAt(1) - '0';

        return Coordinate.of(row, column);
    }

    private static @NotNull Coordinate to(Move move) {
        String to = move.getTo().toString();
        int column = columnToInt(to.charAt(0));
        int row = to.charAt(1) - '0';

        return Coordinate.of(row, column);
    }

    private static @Nullable Piece getInCaseOfPromotion(Move move) {
        return move.getPromotion() == com.github.bhlangonijr.chesslib.Piece.NONE ? null
                : AlgebraicNotation.fromSymbol(move.getPromotion().getFenSymbol());
    }

    public static int columnToInt(char c) {
        if (c >= 'A' && c <= 'H') {
            return c - 'A' + 1;
        }
        throw new IllegalStateException("Unexpected value: " + c);
    }

    private void calculateSecondPerftValues(long nodes, Move move) {
        Coordinate from = from(move);
        Coordinate to = to(move);
        Piece inCaseOfPromotion = getInCaseOfPromotion(move);

        secondPerftValues.nodes = nodes;
        their_board.undoMove();

        com.github.bhlangonijr.chesslib.Piece pieceOnEndOfMove = their_board
                .getPiece(Square.valueOf(to.toString().toUpperCase()));
        if (!pieceOnEndOfMove.equals(com.github.bhlangonijr.chesslib.Piece.NONE)) {
            secondPerftValues.captures++;
        }

        Square enPassant = their_board.getEnPassant();
        if (!enPassant.equals(Square.NONE) && isCaptureOnPassage(from, to, enPassant)) {
            secondPerftValues.capturesOnPassage++;
            secondPerftValues.captures++;
        }

        com.github.bhlangonijr.chesslib.Piece piece = their_board
                .getPiece(Square.valueOf(from.toString().toUpperCase()));
        final boolean isTheKingMove = piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_KING) ||
                piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_KING);

        final boolean isCastle = (from.equals(Coordinate.e1) &&
                (to.equals(Coordinate.c1) || to.equals(Coordinate.g1))) ||
                ((from.equals(Coordinate.e8)) &&
                        (to.equals(Coordinate.c8) || to.equals(Coordinate.g8)));

        if (isTheKingMove && isCastle) {
            secondPerftValues.castles++;
        }

        if (inCaseOfPromotion != null) {
            secondPerftValues.promotions++;
        }

        their_board.doMove(move);

        final boolean isCheck = their_board.isKingAttacked() && !their_board.isMated();
        if (isCheck) {
            secondPerftValues.checks++;
        }

        if (their_board.isMated()) {
            secondPerftValues.checkMates++;
        }
    }

    private boolean isCaptureOnPassage(Coordinate from, Coordinate to, Square enPassant) {
        final boolean isMoveEndingOnEnPassant = Square.valueOf(to.toString().toUpperCase()).equals(enPassant);
        if (!isMoveEndingOnEnPassant) {
            return false;
        }

        com.github.bhlangonijr.chesslib.Piece piece = their_board
                .getPiece(Square.valueOf(from.toString().toUpperCase()));
        return piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_PAWN) ||
                piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_PAWN);
    }

    private void calculatePerftValues(long nodes) {
        perftValues.nodes = nodes;

        List<String> listOfAlgebraicNotations = our_board.listOfAlgebraicNotations();
        Optional<AlgebraicNotation> notation = our_board.lastAlgebraicNotation();

        if (notation.isEmpty())
            return;

        AlgebraicNotation algebraicNotation = notation.get();
        var lastMove = notation.get().algebraicNotation();

        if (algebraicNotation.isCapture()) {
            perftValues.captures++;
        }

        if (listOfAlgebraicNotations.size() >= 2) {
            String preLastMove = listOfAlgebraicNotations.get(listOfAlgebraicNotations.size() - 2);
            calculateCapturesOnPassage(preLastMove, lastMove);
        }

        if (lastMove.contains("O-O")) {
            perftValues.castles++;
        }

        if (lastMove.contains("=")) {
            perftValues.promotions++;
        }

        if (lastMove.contains("+")) {
            perftValues.checks++;
        }

        if (lastMove.contains("#")) {
            perftValues.checkMates++;
        }
    }

    private void calculateCapturesOnPassage(String preLastMove, String lastMove) {
        if (Objects.isNull(preLastMove)) {
            return;
        }
        if (!lastMove.contains("x")) {
            return;
        }
        if (lastMove.contains("O-O")) {
            return;
        }

        final boolean isPawnMove = Stream.of("K", "Q", "B", "N", "R").noneMatch(lastMove::startsWith) &&
                Stream.of("K", "Q", "B", "N", "R").noneMatch(preLastMove::startsWith);
        if (!isPawnMove) {
            return;
        }

        AlgebraicNotation lastAN = AlgebraicNotation.of(lastMove);
        Pair<Coordinate, Coordinate> coordinatesOfLastMove = lastAN.coordinates();
        final int endOfLastMove = coordinatesOfLastMove.getSecond().row();

        final boolean isCaptureOnEnPassaunLine = endOfLastMove == 3 || endOfLastMove == 6;
        if (!isCaptureOnEnPassaunLine) {
            return;
        }

        AlgebraicNotation preLastAN = AlgebraicNotation.of(preLastMove);

        if (AlgebraicNotation.isCastling(preLastAN) != null) {
            return;
        }

        Pair<Coordinate, Coordinate> coordinatesOfPreLastMove = preLastAN.coordinates();
        final int startOfPreLastMove = coordinatesOfPreLastMove.getFirst().row();
        final int endOfPreLastMove = coordinatesOfPreLastMove.getSecond().row();

        final boolean isNotTheSameColumn = coordinatesOfPreLastMove.getSecond().column() != coordinatesOfLastMove
                .getSecond().column();
        if (isNotTheSameColumn) {
            return;
        }

        if (endOfLastMove == 3) {
            if (startOfPreLastMove != 2) {
                return;
            }

            if (endOfPreLastMove != 4) {
                return;
            }

            perftValues.capturesOnPassage++;
            return;
        }

        if (startOfPreLastMove != 7) {
            return;
        }

        if (endOfPreLastMove != 5) {
            return;
        }

        perftValues.capturesOnPassage++;
    }

    private void assertPerftDepth1() {
        logValues();
        assertEquals(20L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth2() {
        logValues();
        assertEquals(400L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth3() {
        logValues();
        assertEquals(8_902L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth4() {
        logValues();
        assertEquals(197_281L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth5() {
        logValues();
        assertEquals(4_865_609L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth6() {
        logValues();
        assertEquals(119_060_324L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth7() {
        logValues();
        assertEquals(3_195_901_860L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth8() {
        logValues();
        assertEquals(84_998_978_956L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth9() {
        logValues();
        assertEquals(2_439_530_234_167L, perftValues.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth1(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(20L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth2(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(400L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth3(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(8_902L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth4(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(197_281L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth5(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(4_865_609L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth6(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(119_060_324L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth7(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(3_195_901_860L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth8(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(84_998_978_956L, nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth9(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(2_439_530_234_167L, nodes, "Nodes count mismatch");
    }

    private void logValues() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Second nodes count: %d", secondPerftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("Second captures count: %d", secondPerftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Second en passaunt count: %d", secondPerftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Second castles count: %d", secondPerftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Second promotions count: %d", secondPerftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Second checks count: %d", secondPerftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);
        Log.infof("Second checkmates count: %d", secondPerftValues.checkMates);
    }

    static class PerftValues {
        long nodes;
        long captures;
        long capturesOnPassage;
        long castles;
        long promotions;
        long checks;
        long checkMates;

        private PerftValues(long nodes, long captures, long capturesOnPassage,
                long castles, long promotions, long checks, long checkMates) {
            this.nodes = nodes;
            this.captures = captures;
            this.capturesOnPassage = capturesOnPassage;
            this.castles = castles;
            this.promotions = promotions;
            this.checks = checks;
            this.checkMates = checkMates;
        }

        public static PerftValues newInstance() {
            return new PerftValues(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof PerftValues that))
                return false;

            return nodes == that.nodes &&
                    captures == that.captures &&
                    capturesOnPassage == that.capturesOnPassage &&
                    castles == that.castles &&
                    promotions == that.promotions &&
                    checks == that.checks &&
                    checkMates == that.checkMates;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(nodes);
            result = 31 * result + Long.hashCode(captures);
            result = 31 * result + Long.hashCode(capturesOnPassage);
            result = 31 * result + Long.hashCode(castles);
            result = 31 * result + Long.hashCode(promotions);
            result = 31 * result + Long.hashCode(checks);
            result = 31 * result + Long.hashCode(checkMates);
            return result;
        }

        public boolean verify(PerftValues secondPerftValues) {
            if (secondPerftValues == null)
                return false;

            if (nodes != secondPerftValues.nodes)
                return false;
            if (captures != secondPerftValues.captures)
                return false;
            if (capturesOnPassage != secondPerftValues.capturesOnPassage)
                return false;
            if (castles != secondPerftValues.castles)
                return false;
            if (promotions != secondPerftValues.promotions)
                return false;
            if (checks != secondPerftValues.checks)
                return false;
            return checkMates == secondPerftValues.checkMates;
        }
    }
}
