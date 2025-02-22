package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.PlayerMove;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.Password;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPerft {
    public static final int DEPTH = 1;
    private ChessGame chessGame = chessGameSupplier().get();
    private final ChessBoardNavigator navigator = new ChessBoardNavigator(chessGame.getChessBoard());
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername().username();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername().username();
    private final PerftValues perftValues = PerftValues.newInstance();

    @Test
    void performanceTest() {
//        perft(DEPTH);

        chessGame = chessGameSupplier("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 1").get();
        long v = anotherPerft(DEPTH);
        System.out.println("Total nodes -> " + v);
        System.out.println();
        System.out.println();
        System.out.println();
//        if (DEPTH == 1) {
//            assertPerftDepth1();
//            return;
//        }
//
//        if (DEPTH == 3) {
//            assertPerftDepth3();
//            return;
//        }
//
//        if (DEPTH == 6) {
//            assertPerftDepth6();
//            return;
//        }
//
//        if (DEPTH == 9) {
//            assertPerftDepth9();
//            return;
//        }
//
        Log.warnf("Performance test executed at depth {%s} but no assertion was performed.", DEPTH);
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth1() {
        assertEquals(20L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(0L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(0L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth2() {
        assertEquals(400L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(0L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(0L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth3() {
        assertEquals(8_902L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(34L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(12L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth4() {
        assertEquals(197_281L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(1_576L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(469L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(8L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth5() {
        assertEquals(4_865_609L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(82_719L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(258L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(27_351L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(347L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth6() {
        assertEquals(119_060_324L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(2_812_008L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(5_248L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(809_099L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(10_828L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth7() {
        assertEquals(3_195_901_860L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(108_329_926L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(319_617L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(883_453L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(33_103_848L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(435_767L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth8() {
        assertEquals(84_998_978_956L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(3_523_740_106L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(7_187_977L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);

        assertEquals(23_605_205L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(968_981_593L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(9_852_036L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    private void assertPerftDepth9() {
        assertEquals(2_439_530_234_167L, perftValues.nodes, "Nodes count mismatch");
        Log.infof("Nodes count: %d", perftValues.nodes);

        assertEquals(125_208_536_153L, perftValues.captures, "Captures count mismatch");
        Log.infof("Captures count: %d", perftValues.captures);

        assertEquals(319_496_827L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        Log.infof("En Passant captures count: %d", perftValues.promotions);

        assertEquals(1_784_356_000L, perftValues.castles, "Castles count mismatch");
        Log.infof("Castles count: %d", perftValues.castles);

        assertEquals(17_334_376L, perftValues.promotions, "Promotions count mismatch");
        Log.infof("Promotions count: %d", perftValues.promotions);

        assertEquals(36_095_901_903L, perftValues.checks, "Checks count mismatch");
        Log.infof("Checks count: %d", perftValues.checks);

        assertEquals(400_191_963L, perftValues.checkMates, "Checkmates count mismatch");
        Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    void perft(int depth) {
        if (depth == 0) {
            return;
        }

        Set<PlayerMove> validMoves = chessGame.getChessBoard().generateValidMoves();
        for (PlayerMove move : validMoves) {
            processingOfTheMove(move);
            perft(depth - 1);
            chessGame.returnMovement(usernameOfPlayerForWhites);
            chessGame.returnMovement(usernameOfPlayerForBlacks);
        }
    }

    long anotherPerft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        Set<PlayerMove> validMoves = chessGame.getChessBoard().generateValidMoves();

        if (depth == DEPTH) {
            for (var move : validMoves) {
                individualNodes.put(move.toString(), 0L);
            }
        }

        for (PlayerMove move : validMoves) {
            String activePlayerUsername = chessGame.getPlayersTurn().equals(Color.BLACK) ? usernameOfPlayerForBlacks : usernameOfPlayerForWhites;
            Coordinate from = move.from();
            Coordinate to = move.to();
            Piece inCaseOfPromotion = move.promotion();

            chessGame.makeMovement(activePlayerUsername, from, to, inCaseOfPromotion);

            PerftValues valuesOfLastHalfMove = calculatePerftValues();
            perftValues.accumulate(valuesOfLastHalfMove);

            long newNodes = anotherPerft(depth - 1);
            nodes += newNodes;

            if (depth == DEPTH) {
                System.out.printf("%s -> %s | %s\n", move, newNodes, chessGame.getChessBoard().actualRepresentationOfChessBoard());
            }
            chessGame.returnMovement(usernameOfPlayerForWhites);
            chessGame.returnMovement(usernameOfPlayerForBlacks);
        }

        return nodes;
    }

    private void processingOfTheMove(PlayerMove move) {
        String activePlayerUsername = chessGame.getPlayersTurn().equals(Color.BLACK) ? usernameOfPlayerForBlacks : usernameOfPlayerForWhites;
        Coordinate from = move.from();
        Coordinate to = move.to();
        Piece inCaseOfPromotion = move.promotion();

        chessGame.makeMovement(activePlayerUsername, from, to, inCaseOfPromotion);
        calculatePerftValues();
    }

    private void calculatePerftValues() {
        perftValues.nodes++;

        List<String> listOfAlgebraicNotations = chessGame.getChessBoard().listOfAlgebraicNotations();
        String lastMove = listOfAlgebraicNotations.getLast();
        if (Objects.isNull(lastMove)) {
            return;
        }

        if (lastMove.contains("x")) {
            perftValues.captures++;
        }

        if (listOfAlgebraicNotations.size() >= 2) {
            String preLastMove = listOfAlgebraicNotations.get(listOfAlgebraicNotations.size() - 2);
            calculateCapturesOnPassage(preLastMove, lastMove, perftValues);
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

    private static void calculateCapturesOnPassage(String preLastMove, String lastMove, PerftValues tempValues) {
        if (Objects.isNull(preLastMove)) {
            return;
        }
        if (!lastMove.contains("x")) {
            return;
        }
        if (lastMove.contains("O-O")) {
            return;
        }

        final boolean isNotPawnMove = Stream.of("K", "Q", "B", "N", "R").noneMatch(lastMove::startsWith) ||
                Stream.of("K", "Q", "B", "N", "R").noneMatch(preLastMove::startsWith);
        if (isNotPawnMove) {
            return;
        }

        AlgebraicNotation lastAN = AlgebraicNotation.of(lastMove);
        Pair<Coordinate, Coordinate> coordinatesOfLastMove = lastAN.coordinates();
        final int endOfLastMove = coordinatesOfLastMove.getSecond().getRow();

        final boolean isCaptureOnEnPassaunLine = endOfLastMove == 3 || endOfLastMove == 6;
        if (!isCaptureOnEnPassaunLine) {
            return;
        }

        AlgebraicNotation preLastAN = AlgebraicNotation.of(preLastMove);
        Pair<Coordinate, Coordinate> coordinatesOfPreLastMove = preLastAN.coordinates();
        final int startOfPreLastMove = coordinatesOfPreLastMove.getFirst().getRow();
        final int endOfPreLastMove = coordinatesOfPreLastMove.getSecond().getRow();

        final boolean isNotTheSameColumn = coordinatesOfPreLastMove.getSecond().getColumn() != coordinatesOfLastMove.getSecond().getColumn();
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

            tempValues.capturesOnPassage++;
            return;
        }

        if (startOfPreLastMove != 7) {
            return;
        }

        if (endOfPreLastMove != 5) {
            return;
        }

        tempValues.capturesOnPassage++;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class PerftValues {
        long nodes;
        long captures;
        long capturesOnPassage;
        long castles;
        long promotions;
        long checks;
        long checkMates;

        public static PerftValues newInstance() {
            return new PerftValues(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    static Supplier<ChessGame> chessGameSupplier(String fen) {
        final ChessBoard chessBoard = ChessBoard.pureChessFromPosition(fen);

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer", "firstplayer@domai.com").get(),
                userAccountSupplier("secondPlayer", "secondplayer@domai.com").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT,
                false
        );
    }

    static Supplier<ChessGame> chessGameSupplier() {
        final ChessBoard chessBoard = ChessBoard.pureChess();

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer", "firstplayer@domai.com").get(),
                userAccountSupplier("secondPlayer", "secondplayer@domai.com").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT,
                false
        );
    }

    static Supplier<UserAccount> userAccountSupplier(String username, String email) {
        return () -> UserAccount.of(new Username(username), new Email(email), new Password("password"));
    }
}
