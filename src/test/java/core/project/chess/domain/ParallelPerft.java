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
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.PersonalData;
import io.quarkus.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("For technical reasons. Need to be executed separately.")
class ParallelPerft {

    public static String DEFAULT_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static int DEPTH = 4;

    @Test
    void parallelPerftTest() {
        System.out.printf("Current FEN -> %s \n", DEFAULT_FEN);
        ParallelPerftRunner runner = new ParallelPerftRunner(DEPTH, DEFAULT_FEN);
        var pair = runner.start();

        var ourValues = pair.getFirst();
        var theirValues = pair.getSecond();

        switch (DEPTH) {
            case 1 -> assertPerftDepth1(ourValues, theirValues);
            case 2 -> assertPerftDepth2(ourValues, theirValues);
            case 3 -> assertPerftDepth3(ourValues, theirValues);
            case 4 -> assertPerftDepth4(ourValues, theirValues);
            case 5 -> assertPerftDepth5(ourValues, theirValues);
            case 6 -> assertPerftDepth6(ourValues, theirValues);
            case 7 -> assertPerftDepth7(ourValues, theirValues);
        }
    }

    private void assertPerftDepth1(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(20L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth2(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(400L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth3(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(8_902L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth4(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(197_281L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth5(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(4_865_609L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth6(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(119_060_324L, ourData.nodes, "Nodes count mismatch");
    }

    private void assertPerftDepth7(PerftValues ourData, PerftValues theirData) {
        logValues(ourData, theirData);
        assertEquals(3_195_901_860L, ourData.nodes, "Nodes count mismatch");
    }

    private void logValues(PerftValues ourData, PerftValues theirData) {
        Log.infof("Nodes count: %d", ourData.nodes);
        Log.infof("Second nodes count: %d", theirData.nodes);
        Log.infof("Captures count: %d", ourData.captures);
        Log.infof("Second captures count: %d", theirData.captures);
        Log.infof("En Passant captures count: %d", ourData.capturesOnPassage);
        Log.infof("Second en passaunt count: %d", theirData.capturesOnPassage);
        Log.infof("Castles count: %d", ourData.castles);
        Log.infof("Second castles count: %d", theirData.castles);
        Log.infof("Promotions count: %d", ourData.promotions);
        Log.infof("Second promotions count: %d", theirData.promotions);
        Log.infof("Checks count: %d", ourData.checks);
        Log.infof("Second checks count: %d", theirData.checks);
        Log.infof("Checkmates count: %d", ourData.checkMates);
        Log.infof("Second checkmates count: %d", theirData.checkMates);
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

        public void accumulate(PerftValues data) {
            this.nodes += data.nodes;
            this.captures += data.captures;
            this.capturesOnPassage += data.capturesOnPassage;
            this.castles += data.castles;
            this.promotions += data.promotions;
            this.checks += data.checks;
            this.checkMates += data.checkMates;
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
            if (secondPerftValues == null) {
                return false;
            }
            if (nodes != secondPerftValues.nodes) {
                return false;
            }
            if (captures != secondPerftValues.captures) {
                return false;
            }
            if (capturesOnPassage != secondPerftValues.capturesOnPassage) {
                return false;
            }
            if (castles != secondPerftValues.castles) {
                return false;
            }
            if (promotions != secondPerftValues.promotions) {
                return false;
            }
            if (checks != secondPerftValues.checks) {
                return false;
            }
            return checkMates == secondPerftValues.checkMates;
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
                false);
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
                false);
    }

    static Supplier<UserAccount> userAccountSupplier(String username, String email) {
        return () -> UserAccount.of(new PersonalData(
                "generateFirstname",
                "generateSurname",
                username,
                email,
                "password"));
    }

    class ParallelPerftRunner {
        private final ExecutorService executor;

        private final int DEPTH;
        private final String INITIAL_FEN;

        public ParallelPerftRunner(int depth, String fen) {
            this.DEPTH = depth;
            this.INITIAL_FEN = fen;

            // Use Runtime.getRuntime().availableProcessors() for dynamic thread pool sizing
            this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        public Pair<PerftValues, PerftValues> start() {
            try {
                Log.info("Starting parallel runner");
                Log.infof("Available processors: %s", Runtime.getRuntime().availableProcessors());
                Log.infof("FEN: %s", INITIAL_FEN);
                Log.infof("Depth: %s", DEPTH);

                ChessGame game = chessGameSupplier(INITIAL_FEN).get();
                Board initialBoard = new Board();
                initialBoard.loadFromFen(INITIAL_FEN);

                List<Move> legalMoves = initialBoard.legalMoves();
                Log.info("Preparing to analyze " + legalMoves.size() + " moves");

                List<CompletableFuture<Pair<PerftValues, PerftValues>>> pendingTasks = legalMoves.stream()
                        .map(move -> CompletableFuture.supplyAsync(() -> processMove(game, initialBoard, move),
                                executor))
                        .collect(Collectors.toList());

                // Combine and aggregate results
                PerftValues ourPerftResults = PerftValues.newInstance();
                PerftValues theirPerftResults = PerftValues.newInstance();

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        pendingTasks.toArray(new CompletableFuture[0]));

                // Aggregate results as futures complete
                allFutures.thenRun(() -> pendingTasks.stream()
                        .map(CompletableFuture::join)
                        .forEach(pair -> {
                            ourPerftResults.accumulate(pair.getFirst());
                            theirPerftResults.accumulate(pair.getSecond());
                        })).join();

                return Pair.of(ourPerftResults, theirPerftResults);
            } finally {
                // Ensure executor is always shut down
                shutdownExecutor();
            }
        }

        private Pair<PerftValues, PerftValues> processMove(ChessGame originalGame, Board originalBoard, Move move) {
            // Create copies to avoid state mutation
            ChessGame game = copyGame(originalGame);
            Board board = originalBoard.clone();

            String activePlayer = determineActivePlayer(game);
            Coordinate from = from(move);
            Coordinate to = to(move);
            Piece promotionPiece = getInCaseOfPromotion(move);

            // Perform the move
            board.doMove(move);
            game.doMove(activePlayer, from, to, promotionPiece);

            String newFen = game.fen();

            // Create and run perft task for the new board state
            ChessGame nestedGame = chessGameSupplier(newFen).get();
            Board nestedBoard = new Board();
            nestedBoard.loadFromFen(newFen);

            PerftTask task = new PerftTask(nestedGame, nestedBoard, DEPTH - 1);
            var results = task.call();
            Log.infof("%s -> %s \t| %s", move, results.getFirst().nodes, newFen);
            return results;
        }

        private ChessGame copyGame(ChessGame originalGame) {
            String fen = originalGame.fen();
            return chessGameSupplier(fen).get();
        }

        private static @Nullable Piece getInCaseOfPromotion(Move move) {
            return move.getPromotion().getFenSymbol().equals(".") ? null
                    : AlgebraicNotation.fromSymbol(move.getPromotion().getFenSymbol());
        }

        private static @NotNull Coordinate to(Move move) {
            return Coordinate.valueOf(move.getTo().toString().toLowerCase());
        }

        private static @NotNull Coordinate from(Move move) {
            return Coordinate.valueOf(move.getFrom().toString().toLowerCase());
        }

        private String determineActivePlayer(ChessGame game) {
            var whitePlayer = game.getWhitePlayer().getUsername();
            var blackPlayer = game.getBlackPlayer().getUsername();

            return game.getPlayersTurn().equals(Color.WHITE) ? whitePlayer : blackPlayer;
        }

        private void shutdownExecutor() {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    Log.warn("Executor did not terminate cleanly");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        class PerftTask implements Callable<Pair<PerftValues, PerftValues>> {

            private PerftValues ourPerftData = PerftValues.newInstance();
            private PerftValues theirPerftData = PerftValues.newInstance();

            private final ChessGame ourGame;
            private final Board theirBoard;

            private final int DEPTH;

            public PerftTask(ChessGame ourGame, Board theirBoard, int depth) {
                this.ourGame = ourGame;
                this.theirBoard = theirBoard;
                this.DEPTH = depth;
            }

            @Override
            public Pair<PerftValues, PerftValues> call() {
                try {
                    doPerft(ourGame, theirBoard, DEPTH);
                    return Pair.of(ourPerftData, theirPerftData);
                } catch (Exception e) {
                    Log.error("Error during perft calculation: " + e.getMessage());
                    throw new CompletionException(e);
                }
            }

            private long doPerft(ChessGame game, Board board, int currentDepth) {
                if (currentDepth == 0) {
                    return 1L;
                }

                List<Move> moves = board.legalMoves();
                long nodes = 0L;

                for (Move move : moves) {
                    ChessGame gameCopy = copyGame(game);
                    Board boardCopy = board.clone();

                    String activePlayer = determineActivePlayer(gameCopy);
                    Coordinate from = from(move);
                    Coordinate to = to(move);
                    Piece inCaseOfPromotion = getInCaseOfPromotion(move);

                    try {
                        boardCopy.doMove(move);
                        gameCopy.doMove(activePlayer, from, to, inCaseOfPromotion);

                        long newNodes = doPerft(gameCopy, boardCopy, currentDepth - 1);
                        nodes += newNodes;

                        updatePerftData(gameCopy, boardCopy, move, nodes);
                        verifyPerftData(gameCopy, boardCopy, move);
                    } catch (IllegalArgumentException e) {
                        Log.error(String.format(
                                "Invalid move processing: %s | FEN: %s | Error: %s",
                                move,
                                gameCopy.fen(),
                                e.getMessage()));
                    }
                }

                return nodes;
            }

            private void updatePerftData(ChessGame game, Board board, Move move, long nodes) {
                updateOurPerftData(game, nodes);
                updateTheirPerftData(board, move, nodes);
            }

            private void updateOurPerftData(ChessGame game, long nodes) {
                List<String> notations = game.listOfAlgebraicNotations();
                game.lastAlgebraicNotation().ifPresent(notation -> {
                    String lastMove = notation.algebraicNotation();

                    ourPerftData.nodes = nodes;
                    ourPerftData.captures += lastMove.contains("x") ? 1 : 0;
                    ourPerftData.castles += lastMove.contains("O-O") ? 1 : 0;
                    ourPerftData.promotions += lastMove.contains("=") ? 1 : 0;
                    ourPerftData.checks += lastMove.contains("+") ? 1 : 0;
                    ourPerftData.checkMates += lastMove.contains("#") ? 1 : 0;

                    // En passant calculation remains the same
                    if (notations.size() >= 2) {
                        calculateCapturesOnPassage(
                                notations.get(notations.size() - 2),
                                lastMove);
                    }
                });
            }

            private void updateTheirPerftData(Board board, Move move, long nodes) {
                Coordinate from = from(move);
                Coordinate to = to(move);

                board.undoMove(); // Temporary undo to inspect board state

                theirPerftData.nodes = nodes;

                // Capture detection
                com.github.bhlangonijr.chesslib.Piece endPiece = board.getPiece(
                        Square.valueOf(to.toString().toUpperCase()));
                theirPerftData.captures += endPiece != com.github.bhlangonijr.chesslib.Piece.NONE ? 1 : 0;

                // En passant detection
                Square enPassant = board.getEnPassant();
                if (!enPassant.equals(Square.NONE) && isCaptureOnPassage(board, from, to, enPassant)) {
                    theirPerftData.capturesOnPassage++;
                    theirPerftData.captures++;
                }

                // Castle and promotion detection
                com.github.bhlangonijr.chesslib.Piece piece = board.getPiece(
                        Square.valueOf(from.toString().toUpperCase()));
                boolean isKingMove = piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_KING) ||
                        piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_KING);

                boolean isCastle = (from.equals(Coordinate.e1)
                        && (to.equals(Coordinate.c1) || to.equals(Coordinate.g1))) ||
                        (from.equals(Coordinate.e8) && (to.equals(Coordinate.c8) || to.equals(Coordinate.g8)));

                theirPerftData.castles += (isKingMove && isCastle) ? 1 : 0;
                theirPerftData.promotions += (getInCaseOfPromotion(move) != null) ? 1 : 0;

                board.doMove(move); // Restore move

                // Check and checkmate detection
                theirPerftData.checks += (board.isKingAttacked() && !board.isMated()) ? 1 : 0;
                theirPerftData.checkMates += board.isMated() ? 1 : 0;
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
                final int endOfLastMove = coordinatesOfLastMove.getSecond().row();

                final boolean isCaptureOnEnPassaunLine = endOfLastMove == 3 || endOfLastMove == 6;
                if (!isCaptureOnEnPassaunLine) {
                    return;
                }

                AlgebraicNotation preLastAN = AlgebraicNotation.of(preLastMove);
                Pair<Coordinate, Coordinate> coordinatesOfPreLastMove = preLastAN.coordinates();
                final int startOfPreLastMove = coordinatesOfPreLastMove.getFirst().row();
                final int endOfPreLastMove = coordinatesOfPreLastMove.getSecond().row();

                final boolean isNotTheSameColumn = coordinatesOfPreLastMove.getSecond().column() != coordinatesOfLastMove.getSecond().column();
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

                    ourPerftData.capturesOnPassage++;
                    return;
                }

                if (startOfPreLastMove != 7) {
                    return;
                }

                if (endOfPreLastMove != 5) {
                    return;
                }

                ourPerftData.capturesOnPassage++;
            }

            private boolean isCaptureOnPassage(Board board, Coordinate from, Coordinate to, Square enPassant) {
                final boolean isMoveEndingOnEnPassant = Square.valueOf(to.toString().toUpperCase()).equals(enPassant);
                if (!isMoveEndingOnEnPassant) {
                    return false;
                }

                com.github.bhlangonijr.chesslib.Piece piece = board
                        .getPiece(Square.valueOf(from.toString().toUpperCase()));
                return piece.equals(com.github.bhlangonijr.chesslib.Piece.WHITE_PAWN) ||
                        piece.equals(com.github.bhlangonijr.chesslib.Piece.BLACK_PAWN);
            }

            private void verifyPerftData(ChessGame game, Board board, Move move) {
                if (!ourPerftData.verify(theirPerftData)) {
                    Log.error(String.format(
                            "Perft verification failed. Move: %s, FEN: %s, PGN: %s",
                            move,
                            game.fen(),
                            game.pgn()));

                    // Detailed logging of discrepancies
                    logPerftDataDetails();

                    throw new IllegalStateException("Invalid perft values");
                }
            }

            private void logPerftDataDetails() {
                // Detailed logging of all perft data metrics
                Log.error(String.format("""

                        Nodes:       %d vs %d
                        Captures:    %d vs %d
                        En Passant:  %d vs %d
                        Castles:     %d vs %d
                        Promotions:  %d vs %d
                        Checks:      %d vs %d
                        Checkmates:  %d vs %d""",
                        ourPerftData.nodes, theirPerftData.nodes,
                        ourPerftData.captures, theirPerftData.captures,
                        ourPerftData.capturesOnPassage, theirPerftData.capturesOnPassage,
                        ourPerftData.castles, theirPerftData.castles,
                        ourPerftData.promotions, theirPerftData.promotions,
                        ourPerftData.checks, theirPerftData.checks,
                        ourPerftData.checkMates, theirPerftData.checkMates));
            }

        }
    }
}
