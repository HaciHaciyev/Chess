package core.project.chess.domain;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.Password;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPerft {
    public static final int DEPTH = 3;
    private final Board board = new Board();
    private final ChessGame chessGame = chessGameSupplier().get();
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername().username();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername().username();
    private final PerftValues perftValues = PerftValues.newInstance();
    private final PerftValues secondPerftValues = PerftValues.newInstance();

    @Test
    void performanceTest() {
        System.out.printf("Current FEN -> %s \n", chessGame.getChessBoard().actualRepresentationOfChessBoard());
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

    private void assertPerftDepth1() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(20L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(0L, perftValues.captures, "Captures count mismatch");
        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(0L, perftValues.checks, "Checks count mismatch");
        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth2() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(400L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(0L, perftValues.captures, "Captures count mismatch");
        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(0L, perftValues.checks, "Checks count mismatch");
        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth3() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(8_902L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(34L, perftValues.captures, "Captures count mismatch");
        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(12L, perftValues.checks, "Checks count mismatch");
        assertEquals(0L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth4() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(197_281L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(1_576L, perftValues.captures, "Captures count mismatch");
        assertEquals(0L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(469L, perftValues.checks, "Checks count mismatch");
        assertEquals(8L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth5() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(4_865_609L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(82_719L, perftValues.captures, "Captures count mismatch");
        assertEquals(258L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(27_351L, perftValues.checks, "Checks count mismatch");
        assertEquals(347L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth6() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(119_060_324L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(2_812_008L, perftValues.captures, "Captures count mismatch");
        assertEquals(5_248L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(0L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(809_099L, perftValues.checks, "Checks count mismatch");
        assertEquals(10_828L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth7() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(3_195_901_860L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(108_329_926L, perftValues.captures, "Captures count mismatch");
        assertEquals(319_617L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(883_453L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(33_103_848L, perftValues.checks, "Checks count mismatch");
        assertEquals(435_767L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth8() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.capturesOnPassage);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(84_998_978_956L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(3_523_740_106L, perftValues.captures, "Captures count mismatch");
        assertEquals(7_187_977L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(23_605_205L, perftValues.castles, "Castles count mismatch");
        assertEquals(0L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(968_981_593L, perftValues.checks, "Checks count mismatch");
        assertEquals(9_852_036L, perftValues.checkMates, "Checkmates count mismatch");
    }

    private void assertPerftDepth9() {
        Log.infof("Nodes count: %d", perftValues.nodes);
        Log.infof("Captures count: %d", perftValues.captures);
        Log.infof("En Passant captures count: %d", perftValues.promotions);
        Log.infof("Castles count: %d", perftValues.castles);
        Log.infof("Promotions count: %d", perftValues.promotions);
        Log.infof("Checks count: %d", perftValues.checks);
        Log.infof("Checkmates count: %d", perftValues.checkMates);

        assertEquals(2_439_530_234_167L, perftValues.nodes, "Nodes count mismatch");
        assertEquals(125_208_536_153L, perftValues.captures, "Captures count mismatch");
        assertEquals(319_496_827L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        assertEquals(1_784_356_000L, perftValues.castles, "Castles count mismatch");
        assertEquals(17_334_376L, perftValues.promotions, "Promotions count mismatch");
        assertEquals(36_095_901_903L, perftValues.checks, "Checks count mismatch");
        assertEquals(400_191_963L, perftValues.checkMates, "Checkmates count mismatch");
    }

    long perft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        String fen = this.board.getFen();
        List<Move> validMoves = this.board.legalMoves();

        for (Move move : validMoves) {
            String activePlayerUsername = getActivePlayerUsername();
            Coordinate from = from(move);
            Coordinate to = to(move);
            Piece inCaseOfPromotion = getInCaseOfPromotion(move);

            board.doMove(move);
            chessGame.makeMovement(activePlayerUsername, from, to, inCaseOfPromotion);

            long newNodes = perft(depth - 1);
            nodes += newNodes;
            calculatePerftValues(nodes);
            calculateSecondPerftValues(nodes, move);
            verifyPerft(from, to, inCaseOfPromotion);

            if (depth == DEPTH) {
                System.out.printf("%s -> %s \t|\t %s\n", move, newNodes, chessGame.getChessBoard().actualRepresentationOfChessBoard());
            }

            board.undoMove();
            chessGame.returnMovement(usernameOfPlayerForWhites);
            chessGame.returnMovement(usernameOfPlayerForBlacks);
        }

        return nodes;
    }

    private void verifyPerft(Coordinate from, Coordinate to, Piece inCaseOfPromotion) {
        if (perftValues.equals(secondPerftValues)) {
            return;
        }

        Log.errorf("Perft failed. On move: from - %s, to - %s, inCaseOfPromotion - %s", from, to, inCaseOfPromotion);
        Log.errorf("Our ChessBoard: FEN: %s, PGN: %s", Arrays.toString(chessGame.getChessBoard().arrayOfFEN()), chessGame.getChessBoard().pgn());
        System.out.println();
        Log.errorf("External Board: %s FEN: %s", board.getFen());

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
    }

    private static @Nullable Piece getInCaseOfPromotion(Move move) {
        return move.getPromotion().getFenSymbol().equals(".") ? null : AlgebraicNotation.fromSymbol(move.getPromotion().getFenSymbol());
    }

    private static @NotNull Coordinate to(Move move) {
        return Coordinate.valueOf(move.getTo().toString().toLowerCase());
    }

    private static @NotNull Coordinate from(Move move) {
        return Coordinate.valueOf(move.getFrom().toString().toLowerCase());
    }

    private String getActivePlayerUsername() {
        return chessGame.getPlayersTurn().equals(Color.BLACK) ? usernameOfPlayerForBlacks : usernameOfPlayerForWhites;
    }

    private void calculateSecondPerftValues(long nodes, Move move) {
        Coordinate from = from(move);
        Coordinate to = to(move);
        Piece inCaseOfPromotion = getInCaseOfPromotion(move);

        secondPerftValues.nodes = nodes;
        board.undoMove();

        com.github.bhlangonijr.chesslib.Piece pieceOnEndOfMove = board.getPiece(Square.valueOf(to.toString().toUpperCase()));
        if (!pieceOnEndOfMove.equals(com.github.bhlangonijr.chesslib.Piece.NONE)) {
            secondPerftValues.captures++;
        }

        Square enPassant = board.getEnPassant();
        if (!enPassant.equals(Square.NONE) && isCaptureOnPassage(from, to, enPassant)) {
            secondPerftValues.capturesOnPassage++;
        }

        com.github.bhlangonijr.chesslib.Piece piece = board.getPiece(Square.valueOf(from.toString().toUpperCase()));
        final boolean isTheKingMove = piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_KING) ||
                piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_KING);

        final boolean isCastle = isTheKingMove && (from.equals(Coordinate.e1) &&
                (to.equals(Coordinate.c1) || to.equals(Coordinate.g1))) ||
                (from.equals(Coordinate.e8)) &&
                (to.equals(Coordinate.c8) || to.equals(Coordinate.g8));

        if (isCastle) {
            secondPerftValues.castles++;
        }

        if (inCaseOfPromotion != null) {
            secondPerftValues.promotions++;
        }

        board.doMove(move);

        final boolean isCheck = board.isKingAttacked() && !board.isMated();
        if (isCheck) {
            secondPerftValues.checks++;
        }

        if (board.isMated()) {
            secondPerftValues.checkMates++;
        }
    }

    private boolean isCaptureOnPassage(Coordinate from, Coordinate to, Square enPassant) {
        final boolean isMoveEndingOnEnPassant = Square.valueOf(to.toString().toUpperCase()).equals(enPassant);
        if (!isMoveEndingOnEnPassant) {
            return false;
        }

        com.github.bhlangonijr.chesslib.Piece piece = board.getPiece(Square.valueOf(from.toString().toUpperCase()));
        return piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_PAWN) ||
                piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_PAWN);
    }

    private void calculatePerftValues(long nodes) {
        perftValues.nodes = nodes;

        List<String> listOfAlgebraicNotations = chessGame.getChessBoard().listOfAlgebraicNotations();
        Optional<AlgebraicNotation> notation = chessGame.getChessBoard().lastAlgebraicNotation();

        if (notation.isEmpty()) return;

        var lastMove = notation.get().algebraicNotation();

        if (lastMove.contains("x")) {
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

        final boolean isPawnMove = Stream.of("K", "Q", "B", "N", "R").noneMatch(lastMove::startsWith) ||
                Stream.of("K", "Q", "B", "N", "R").noneMatch(preLastMove::startsWith);
        if (!isPawnMove) {
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

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof PerftValues that)) return false;

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
