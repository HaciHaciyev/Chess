package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Piece;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPerft {
    public static final int DEPTH = 1;
    private final ChessGame chessGame = chessGameSupplier().get();
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername().username();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername().username();
    private final PerftValues perftValues = PerftValues.newInstance();

    @Test
    void performanceTest() {
        perft(DEPTH);

        if (DEPTH == 1) {
            assertPerftDepth1();
            return;
        }

        if (DEPTH == 3) {
            assertPerftDepth3();
            return;
        }

        if (DEPTH == 6) {
            assertPerftDepth6();
            return;
        }

        if (DEPTH == 9) {
            assertPerftDepth9();
            return;
        }

        Log.warnf("Performance test executed at depth {%s} but no assertion was performed.", DEPTH);
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
            perftValues.nodes++;
            return;
        }

        Set<PlayerMove> validMoves = chessGame.getChessBoard().generateValidMoves();
        for (PlayerMove move : validMoves) {
            processingOfTheMove(move);
        }
    }

    private void processingOfTheMove(PlayerMove move) {
        String activePlayerUsername = chessGame.getPlayersTurn().equals(Color.BLACK) ? usernameOfPlayerForBlacks : usernameOfPlayerForWhites;
        Coordinate from = move.from();
        Coordinate to = move.to();
        Piece inCaseOfPromotion = move.promotion();

        chessGame.makeMovement(activePlayerUsername, from, to, inCaseOfPromotion);
        PerftValues tempValues = calculatePerftValues();
        perftValues.accumulate(tempValues);

        chessGame.returnMovement(usernameOfPlayerForWhites);
        chessGame.returnMovement(usernameOfPlayerForBlacks);
    }

    private PerftValues calculatePerftValues() {
        PerftValues tempValues = PerftValues.newInstance();
        tempValues.nodes++;

        List<String> listOfAlgebraicNotations = chessGame.getChessBoard().listOfAlgebraicNotations();
        String lastMove = listOfAlgebraicNotations.getLast();
        if (Objects.isNull(lastMove)) {
            return null;
        }

        if (lastMove.contains("x")) {
            tempValues.captures++;
        }

        String preLastMove = listOfAlgebraicNotations.get(listOfAlgebraicNotations.size() - 2);
        calculateCapturesOnPassage(preLastMove, lastMove, tempValues);

        if (lastMove.contains("O-O")) {
            tempValues.castles++;
        }

        if (lastMove.contains("=")) {
            tempValues.promotions++;
        }

        if (lastMove.contains("+")) {
            tempValues.checks++;
        }

        if (lastMove.contains("#")) {
            tempValues.checkMates++;
        }

        return tempValues;
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

        public void accumulate(PerftValues other) {
            this.nodes += other.nodes;
            this.captures += other.captures;
            this.capturesOnPassage += other.capturesOnPassage;
            this.castles += other.castles;
            this.promotions += other.promotions;
            this.checks += other.checks;
            this.checkMates += other.checkMates;
        }
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
