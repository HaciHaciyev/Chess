package core.project.chess.domain.Perft;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;
import testUtils.PerftUtil;

public class InfoPerftTest {
    private int DEPTH = 0;
    private ChessBoard our_board;
    private Board their_board;

    private PerftValues perftValues;
    private PerftValues secondPerftValues;

    private Deque<String> move_stack;
    private Deque<String> undo_stack;

    @Test
    void perftStandartPosition() {
        DEPTH = 6;

        System.out.println("Perft standart position");

        move_stack = new ArrayDeque<>();
        undo_stack = new ArrayDeque<>();

        our_board = ChessBoard.pureChess();
        their_board = new Board();

        perftValues = PerftValues.newInstance();
        secondPerftValues = PerftValues.newInstance();

        long nodes = perft(DEPTH);
        System.out.println("Nodes: " + nodes);
        switch (DEPTH) {
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
        DEPTH = 4;

        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        System.out.println("Perft good position: " + fen);

        move_stack = new ArrayDeque<>();
        undo_stack = new ArrayDeque<>();

        our_board = ChessBoard.pureChessFromPosition(fen);
        their_board = new Board();
        their_board.loadFromFen(fen);

        perftValues = PerftValues.newInstance();
        secondPerftValues = PerftValues.newInstance();

        long nodes = perft(DEPTH);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    void perftPromotionPosition() {
        DEPTH = 2;

        String fen = "n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1";
        System.out.println("Perft promotion position: " + fen);

        move_stack = new ArrayDeque<>();
        undo_stack = new ArrayDeque<>();

        our_board = ChessBoard.pureChessFromPosition(fen);
        their_board = new Board();
        their_board.loadFromFen(fen);

        perftValues = PerftValues.newInstance();
        secondPerftValues = PerftValues.newInstance();

        long nodes = perft(DEPTH);
        System.out.println("Nodes: " + nodes);
    }

    @Test
    @Disabled
    void perftCustomPosition() {
        DEPTH = 0;

        String fen = "";
        System.out.println("Perft custom position: " + fen);

        move_stack = new ArrayDeque<>();
        undo_stack = new ArrayDeque<>();

        our_board = ChessBoard.pureChessFromPosition(fen);
        their_board = new Board();
        their_board.loadFromFen(fen);

        perftValues = PerftValues.newInstance();
        secondPerftValues = PerftValues.newInstance();

        long nodes = perft(DEPTH);
        System.out.println("Nodes: " + nodes);
    }

    long perft(int depth) {
        long nodes = 0L;

        if (depth == 0) {
            return 1L;
        }

        List<core.project.chess.domain.chess.value_objects.Move> our_valid_moves = null;
        List<com.github.bhlangonijr.chesslib.move.Move> their_valid_moves = their_board.legalMoves();

        try {
            our_valid_moves = our_board.generateAllValidMoves();
        } catch (Exception e) {
            System.out.printf("Could not generate moves for position: %s | current depth: %s%n",
                    our_board.actualRepresentationOfChessBoard(), depth);
            throw e;
        }

        if (our_valid_moves.size() != their_valid_moves.size()) {
            System.out.println();
            System.out.println("DEPTH: " + depth);
            System.out.println("MOVE STACK: " + move_stack);
            System.out.println("UNDO STACK: " + undo_stack);
            System.out.println("MOVES HISTORY OF BOARD: " + our_board.listOfAlgebraicNotations());
            System.out.println("OUR FEN: \t" + our_board.actualRepresentationOfChessBoard());
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
                move_stack.push(move.toString());

                String our_fen = our_board.actualRepresentationOfChessBoard();
                String their_fen = their_board.getFen();

                if (!our_fen.equals(their_board.getFen())) {
                    System.out.println("DEPTH: " + depth);
                    System.out.println("MOVE STACK: " + move_stack);
                    System.out.println("UNDO STACK: " + undo_stack);
                    System.out.println("MOVES HISTORY OF BOARD: " + our_board.listOfAlgebraicNotations());
                    System.out.println("OUR FEN: \t" + our_fen);
                    System.out.println("THEIR FEN: \t" + their_fen);
                    System.out.println();
                    PerftUtil.analyze(our_fen, their_fen);
                    throw new RuntimeException("FEN mismatch");
                }
            } catch (Exception e) {
                System.out.printf("Error making move: %s | position: %s | depth: %s%n", move,
                        our_board.actualRepresentationOfChessBoard(), depth);
                throw e;
            }

            long newNodes = perft(depth - 1);
            nodes += newNodes;
            calculatePerftValues(nodes);
            calculateSecondPerftValues(nodes, move);
            verifyPerft(move, from, to, inCaseOfPromotion);

            if (depth == DEPTH) {
                System.out.printf("%s -> %s \t|\t %s\n", move, newNodes, our_board.actualRepresentationOfChessBoard());
            }

            their_board.undoMove();
            our_board.undoMove();
            undo_stack.push(move.toString());

            String our_undo_fen = our_board.actualRepresentationOfChessBoard();
            String their_undo_fen = their_board.getFen();

            if (!our_undo_fen.equals(their_undo_fen)) {
                System.out.println();
                System.out.println("DEPTH: " + depth);
                System.out.println("MOVE STACK: " + move_stack);
                System.out.println("UNDO STACK: " + undo_stack);
                System.out.println("MOVES HISTORY OF BOARD: " + our_board.listOfAlgebraicNotations());
                System.out.println("OUR FEN: \t" + our_undo_fen);
                System.out.println("THEIR FEN: \t" + their_undo_fen);
                System.out.println();
                PerftUtil.analyze(our_undo_fen, their_undo_fen);
                throw new RuntimeException("UNDO FEN mismatch");
            }

            move_stack.pop();
            undo_stack.pop();
        }

        return nodes;
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
}
