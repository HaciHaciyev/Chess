package core.project.chess.domain.subdomains.chess.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.ChessMove;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.Password;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import testUtils.ChessGameFixedThreadExecutor;
import testUtils.SimplePGNReader;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class ChessGameTest {

    @Test
    void pgnUtil() {
        final String pgn = """
                1. d2-d4 Ng8-f6 2. c2-c4 e7-e6 3. Ng1-f3 d7-d5 4. Nb1-c3 c7-c5 5. c4xd5 c5xd4 6. Qd1xd4 e6xd5 7. Bc1-g5 Bf8-e7 8. e2-e3 O-O 9. Bf1-e2 Nb8-c6 10. Qd4-d3 h7-h6 11. Bg5-h4 Qd8-b6 12. O-O Rf8-d8 13. Rf1-d1 Qb6xb2 14. Ra1-b1 Qb2-a3 15. Bh4xf6 Be7xf6 16. Nc3xd5 Qa3xd3 17. Nd5xf6+ g7xf6 18. Rd1xd3 Bc8-f5 19. Rd3xd8+ Nc6xd8 20. Rb1-b2 Ra8-c8 21. h2-h4 Rc8-c1+ 22. Kg1-h2 Rc1-b1 23. Rb2xb1 Bf5xb1 24. a2-a3 Kg8-f8 25. g2-g4 Nd8-e6 26. Kh2-g3 Kf8-e7 27. Nf3-d2 Bb1-c2 28. f2-f4 Ne6-c5 29. Kg3-f3 Nc5-b3 30. Nd2xb3 Bc2xb3 31. Kf3-e4 Bb3-e6 32. Ke4-d4 b7-b6 33. e3-e4 a7-a5 34. e4-e5 f6xe5+ 35. Kd4xe5 f7-f6+ 36. Ke5-d4 Be6-d7 37. g4-g5 f6xg5 38. h4xg5 h6xg5 39. f4xg5 b6-b5 40. Kd4-c5 b5-b4 41. a3xb4 a5xb4 42. Be2-c4 Bd7-e6 43. Kc5xb4 Be6xc4 44. Kb4xc4 Ke7-e6 45. g5-g6 Ke6-f6 46. Kc4-d4 Kf6xg6
                """;

        final String result = pgn.replaceAll("(-)(?![O-])", "");
        Log.info(result);
    }

    @Test
    @Disabled("For single purposes.")
    void temp() {
        executeGamesFromPGN("src/main/resources/pgn/temp.pgn", true, true, true);
    }

    @Test
    @Disabled("...")
    @DisplayName("Test chess game with many invalid moves.")
    void testChessGame() {

        long startTime = System.nanoTime();
        for (int i = 0; i < 150_000; i++) {
            chessGameLoad();
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.println(duration);
    }

    @Test
    @Disabled("For single running.")
    @DisplayName("100k+ games from lichess")
    void lichess_100k() {
        executeGamesFromPGN(
                "src/main/resources/pgn/lichess_2013_january_lalg.pgn",
                false,
                false,
                false);
    }

    @Test
    @DisplayName("100k+ games from lichess Concurrent")
    void lichess_100k_Concurrent() {

        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
                "src/main/resources/pgn/lichess_2013_january_lalg.pgn",
                8,
                128,
                true,
                false,
                false
        );

        assertTrue(executor.start());
    }

    @Test
    @DisplayName("Berliner_PGN_Archive_64")
    @Disabled("no concurrent")
    void berliner64() {
        executeGamesFromPGN(
                "src/main/resources/chess/pgn/Berliner_lalg.pgn",
                false,
                false,
                false);
    }

    @Test
    @DisplayName("Berliner_PGN_Archive_64 Concurrent")
    void berliner64_Concurrent() {

        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
                "src/main/resources/pgn/Berliner_lalg.pgn",
                8,
                8,
                true,
                false,
                false

        );


        assertTrue(executor.start());
    }

    @Test
    @DisplayName("Mamedyarov_PGN_Archive_4684")
    @Disabled("no concurrent")
    void mamedyarov_ALL() {
        executeGamesFromPGN(
                "src/main/resources/chess/pgn/Mamedyarov_lalg.pgn",
                false,
                false,
                false);
    }

    @Test
    @DisplayName("Mamedyarov_PGN_Archive_4684 Concurrent")
    void mamedyarov_ALL_Concurrent() {

        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
                "src/main/resources/pgn/Mamedyarov_lalg.pgn",
                8,
                8,
                true,
                false,
                false
        );

        assertTrue(executor.start());
    }

    @Test
    @DisplayName("Hikaru_PGN_Archive_8025")
    @Disabled("no concurrent")
    void nakamura_ALL() {
        executeGamesFromPGN(
                "src/main/resources/chess/pgn/Hikaru_lalg.pgn",
                false,
                false,
                false);
    }

    @Test
    @DisplayName("Hikaru_PGN_Archive_8025 Concurrent")
    void nakamura_ALL_Concurrent() {
        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
        "src/main/resources/pgn/Hikaru_lalg.pgn",
                8,
                8,
                true,
                false,
                false
        );

        assertTrue(executor.start());
    }

    @Test
    @DisplayName("Magnus ALL")
    @Disabled("no concurrent")
    void magnus_ALL() {
        executeGamesFromPGN(
                "src/main/resources/chess/pgn/Magnus_lalg.pgn",
                false,
                false,
                false);
    }

    @Test
    @DisplayName("Magnus ALL Concurrent fixed thread")
    void magnus_ALL_Concurrent() {
        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
                "src/main/resources/pgn/Magnus_lalg.pgn",
                4,
                4,
                true,
                false,
                false
        );

        assertTrue(executor.start());
    }

    @Test
    @Disabled(",,,")
    @DisplayName("Checkmates from Lichess 2013 January")
    void lichessCheckmates() {
        executeGamesFromPGN(
                "src/main/resources/chess/pgn/lichess_2013_january_checkmates_lalg.pgn",
                false,
                true,
                false);
    }

    @Test
    @DisplayName("Checkmates from Lichess 2013 January Concurrent")
    void lichessCheckmates_Concurrent() {
        ChessGameFixedThreadExecutor executor = new ChessGameFixedThreadExecutor(
                "src/main/resources/pgn/lichess_2013_january_checkmates_lalg.pgn",
                8,
                8,
                true,
                false,
                true
        );

        assertTrue(executor.start());
    }

    @Test
    @DisplayName("Stalemates from Lichess 2013 January")
    void lichessStalemates() {
        executeGamesFromPGN(
                "src/main/resources/pgn/lichess_2013_january_stalemates_lalg.pgn",
                false,
                true,
                false);
    }

    @Test
    @DisplayName("undo move")
    void undoMove() {
        ChessGame game = defaultChessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername().username();
        String black = game.getPlayerForBlack().getUsername().username();

        ChessBoardNavigator navigator = new ChessBoardNavigator(game.getChessBoard());

        game.makeMovement(white, e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e2, e4, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, a7, a6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e4, e5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, d7, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(black, d7, d5, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.makeMovement(white, e5, d6, null);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());

        game.returnMovement(white);
        game.returnMovement(black);
        System.out.println(navigator.prettyToString());
        System.out.println(navigator.board().listOfAlgebraicNotations());
    }

    public static void executeGamesFromPGN(String path, boolean enableLogging, boolean enableAssertions, boolean enablePGN) {
        List<String> strings = SimplePGNReader.extractFromPGN(path);
        int pgnNum = 1;
        for (String pgn : strings) {

            try {
                executeGameFromPGN(pgn, pgnNum, enableLogging, enableAssertions, enablePGN);
            } catch (AssertionFailedError | IllegalStateException e) {
                Log.error(e.getMessage());
                throw new IllegalStateException(e.getMessage());
            }
            pgnNum++;
        }
    }

    public static void executeGameFromPGN(String pgn, int pgnNum, boolean enableLogging, boolean enableAssertions, boolean enablePGN) {
        ChessGame game = defaultChessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername().username();
        String black = game.getPlayerForBlack().getUsername().username();

        ChessBoardNavigator navigator = new ChessBoardNavigator(game.getChessBoard());

        if (enableLogging) {
            Log.infof("Reading game#%s.", pgnNum);
        }

        SimplePGNReader pgnReader = new SimplePGNReader(pgn);
        List<ChessMove> moves = pgnReader.readAll();

        int moveNum = 0;
        for (ChessMove move : moves) {
            if (move.white() == null) {
                break;
            }

            moveNum++;
            if (enableLogging) {
                Log.infof("Cite: %s. Game: %d. Movement: %d.", pgnReader.tag("Site"), pgnNum, moveNum);
            }

            if (enableLogging) {
                Log.infof("White: %s", move.white());
            }
            try {
                var whiteMessage = game.makeMovement(white, move.white().from(), move.white().to(), move.white().promotion());

                if (enableLogging) {
                    Log.infof("Movement result for white: %s", whiteMessage);
                }

                if (enableLogging) {
                    System.out.println(navigator.prettyToString());
                }

                if (move.black() == null) {
                    break;
                }

                if (enableLogging) {
                    Log.info("Black: " + move.black());
                }

                var blackMessage = game.makeMovement(black, move.black().from(), move.black().to(), move.black().promotion());

                if (enableLogging) {
                    System.out.println(navigator.prettyToString());

                    if (enablePGN) {
                        System.out.println();
                        System.out.println(navigator.board().pgn());
                    }
                }

                if (enableLogging) {
                    Log.infof("Movement result for black: %s.", blackMessage);
                }
            } catch (IllegalStateException e) {
                String err = """
                
                %s | Move: %d | Game: %d
                
                %s
                """.formatted(e.getMessage(), moveNum, pgnNum, pgn);
                throw new IllegalStateException(err);
            }
        }

        String result = pgnReader.tag("Result");

        if (enableLogging) {
            Log.infof("Chessland PGN: %s.", game.getChessBoard().pgn());
            Log.info("Result of PGN: " + result);
            Log.info("Game status: " + game.gameResult());
            System.out.println();
        }

        if (enableAssertions) {
            processResult(game, result, moveNum, pgnNum, pgn);
        }
    }

    private static void processResult(ChessGame game, String result, int moveNum, int gameNum, String pgn) {
        if (result.equals("\"1/2-1/2\"")) {
            Assertions.assertTrue(game.gameResult().isPresent(), """
                        
                        --> NO GAME RESULT <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));

            assertEquals(GameResult.DRAW, game.gameResult().orElseThrow(), """
                        
                        --> EXPECTED DRAW <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));
        }

        if (result.equals("\"1-0\"")) {
            Assertions.assertTrue(game.gameResult().isPresent(), """
                        
                        --> NO GAME RESULT <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));


            assertEquals(GameResult.WHITE_WIN, game.gameResult().orElseThrow(), """
                        
                        --> EXPECTED WHITE_WIN <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));
        }

        if (result.equals("\"0-1\"")) {
            Assertions.assertTrue(game.gameResult().isPresent(), """
                        
                        --> NO GAME RESULT <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));

            assertEquals(GameResult.BLACK_WIN, game.gameResult().orElseThrow(), """
                        
                        --> EXPECTED BLACK_WIN <--
                        Move: %s | Game: %s
                        
                        ----------------------
                        
                        %s
                        """.formatted(moveNum, gameNum, pgn));
        }
    }

    @Test
    @DisplayName("Simple testing of FEN.")
    void fenTest() {
        final ChessGame chessGame = defaultChessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        System.out.println(chessGame.getChessBoard().toString());

        // 1
        chessGame.makeMovement(firstPlayerUsername, e2, e4, null);

        System.out.println(chessGame.getChessBoard().toString());

        chessGame.makeMovement(secondPlayerUsername, e7, e5, null);

        System.out.println(chessGame.getChessBoard().toString());
    }

    @Test
    @DisplayName("Chess game end by pat in 10 move.")
    void testChessGameEndByPat() {
        final ChessGame chessGame = defaultChessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        // 1.
        chessGame.makeMovement(firstPlayerUsername, e2, e3, null);

        chessGame.makeMovement(secondPlayerUsername, a7, a5, null);

        // 2.
        chessGame.makeMovement(firstPlayerUsername, d1, h5, null);

        chessGame.makeMovement(secondPlayerUsername, a8, a6, null);

        // 3.
        chessGame.makeMovement(firstPlayerUsername, h5, a5, null);

        chessGame.makeMovement(secondPlayerUsername, h7, h5, null);

        // 4.
        chessGame.makeMovement(firstPlayerUsername, a5, c7, null);

        chessGame.makeMovement(secondPlayerUsername, a6, h6, null);

        // 5.
        chessGame.makeMovement(firstPlayerUsername, h2, h4, null);

        chessGame.makeMovement(secondPlayerUsername, f7, f6, null);

        // 6.
        chessGame.makeMovement(firstPlayerUsername, c7, d7, null);

        chessGame.makeMovement(secondPlayerUsername, e8, f7, null);

        // 7.
        chessGame.makeMovement(firstPlayerUsername, d7, b7, null);

        chessGame.makeMovement(secondPlayerUsername, d8, d3, null);

        // 8.
        chessGame.makeMovement(firstPlayerUsername, b7, b8, null);

        chessGame.makeMovement(secondPlayerUsername, d3, h7, null);

        // 9.
        chessGame.makeMovement(firstPlayerUsername, b8, c8, null);

        chessGame.makeMovement(secondPlayerUsername, f7, g6, null);

        // 10. STALEMATE
        chessGame.makeMovement(firstPlayerUsername, c8, e6, null);

        assertEquals(GameResult.DRAW, chessGame.gameResult().orElseThrow());
    }

    @Test
    @DisplayName("Game between AinGrace and Hadzhy98 on 2024.08.04\nhttps://lichess.org/zuOBpEUY#11")
    void gameOn_2024_08_04() {
        ChessGame game = defaultChessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername().username();
        String blackPlayer = game.getPlayerForBlack().getUsername().username();

        //1.
        game.makeMovement(whitePlayer, e2, e4, null);

        game.makeMovement(blackPlayer, e7, e5, null);

        //2.
        game.makeMovement(whitePlayer, g1, f3, null);

        game.makeMovement(blackPlayer, b8, c6, null);

        //3.
        game.makeMovement(whitePlayer, b1, c3, null);

        game.makeMovement(blackPlayer, g8, f6, null);

        //4.
        game.makeMovement(whitePlayer, d2, d3, null);

        game.makeMovement(blackPlayer, f8, c5, null);

        //5.
        game.makeMovement(whitePlayer, c1, g5, null);

        game.makeMovement(blackPlayer, h7, h6, null);

        //6.
        game.makeMovement(whitePlayer, g5, f6, null);

        game.makeMovement(blackPlayer, d8, f6, null);

        //7.
        game.makeMovement(whitePlayer, a2, a3, null);

        game.makeMovement(blackPlayer, d7, d6, null);

        //8.
        game.makeMovement(whitePlayer, c3, d5, null);

        game.makeMovement(blackPlayer, f6, d8, null);

        //9.
        game.makeMovement(whitePlayer, c2, c3, null);

        game.makeMovement(blackPlayer, c8, e6, null);

        //10.
        game.makeMovement(whitePlayer, d1, a4, null);

        game.makeMovement(blackPlayer, d8, d7, null);

        //11.
        game.makeMovement(whitePlayer, d5, e3, null);

        game.makeMovement(blackPlayer, c5, e3, null);

        //12.
        game.makeMovement(whitePlayer, f2, e3, null);

        game.makeMovement(blackPlayer, e8, g8, null);

        //13.
        game.makeMovement(whitePlayer, d3, d4, null);

        game.makeMovement(blackPlayer, e6, g4, null);

        //14.
        game.makeMovement(whitePlayer, f3, d2, null);

        game.makeMovement(blackPlayer, d7, e7, null);

        //15.
        game.makeMovement(whitePlayer, d4, e5, null);

        game.makeMovement(blackPlayer, d6, e5, null);

        //16.
        game.makeMovement(whitePlayer, d2, c4, null);

        game.makeMovement(blackPlayer, e7, g5, null);

        //17.
        game.makeMovement(whitePlayer, f1, d3, null);

        game.makeMovement(blackPlayer, g4, e6, null);

        //18.
        game.makeMovement(whitePlayer, g2, g3, null);

        game.makeMovement(blackPlayer, e6, c4, null);

        //19.
        game.makeMovement(whitePlayer, d3, c4, null);

        game.makeMovement(blackPlayer, g5, e3, null);

        //20. fixed
        game.makeMovement(whitePlayer, c4, e2, null);

        game.makeMovement(blackPlayer, a7, a6, null);

        //21.
        game.makeMovement(whitePlayer, h1, f1, null);

        game.makeMovement(blackPlayer, b7, b5, null);

        //22.
        game.makeMovement(whitePlayer, a4, c2, null);

        game.makeMovement(blackPlayer, a6, a5, null);

        //23.
        game.makeMovement(whitePlayer, c3, c4, null);

        game.makeMovement(blackPlayer, b5, b4, null);

        //24.
        game.makeMovement(whitePlayer, a3, b4, null);

        game.makeMovement(blackPlayer, c6, b4, null);

        //25.
        game.makeMovement(whitePlayer, c2, b1, null);

        game.makeMovement(blackPlayer, f8, d8, null);

        //26.
        game.makeMovement(whitePlayer, f1, f5, null);

        game.makeMovement(blackPlayer, f7, f6, null);

        //27.
        game.makeMovement(whitePlayer, a1, a3, null);

        game.makeMovement(blackPlayer, e3, d2, null);

        //28.
        game.makeMovement(whitePlayer, e1, f1, null);

        game.makeMovement(blackPlayer, d2, c2, null);

        //29.
        game.makeMovement(whitePlayer, a3, a1, null);

        game.makeMovement(blackPlayer, d8, d4, null);

        //30.
        game.makeMovement(whitePlayer, e2, f3, null);

        game.makeMovement(blackPlayer, c2, c4, null);

        //31.
        game.makeMovement(whitePlayer, f1, g2, null);

        game.makeMovement(blackPlayer, d4, d2, null);

        //32.
        game.makeMovement(whitePlayer, g2, g1, null);

        game.makeMovement(blackPlayer, g8, f7, null);

        //33.
        game.makeMovement(whitePlayer, b1, f1, null);

        game.makeMovement(blackPlayer, c4, f1, null);

        //34.
        game.makeMovement(whitePlayer, g1, f1, null);

        game.makeMovement(blackPlayer, g7, g6, null);

        //35.
        game.makeMovement(whitePlayer, f1, e1, null);

        game.makeMovement(blackPlayer, d2, b2, null);

        //36.
        game.makeMovement(whitePlayer, f3, h5, null);

        game.makeMovement(blackPlayer, b4, c2, null);

        //37.
        game.makeMovement(whitePlayer, e1, f2, null);

        game.makeMovement(blackPlayer, c2, a1, null);

        //38.
        game.makeMovement(whitePlayer, f2, f3, null);

        game.makeMovement(blackPlayer, a8, d8, null);

        //39.
        game.makeMovement(whitePlayer, f3, g4, null);

        game.makeMovement(blackPlayer, c7, c5, null);

        //40.
        game.makeMovement(whitePlayer, h2, h4, null);

        game.makeMovement(blackPlayer, c5, c4, null);

        //41.
        game.makeMovement(whitePlayer, g4, f3, null);

        game.makeMovement(blackPlayer, c4, c3, null);

        //42. checkmate
        game.makeMovement(whitePlayer, g3, g4, null);

        game.makeMovement(blackPlayer, d8, d3, null);
    }

    public void chessGameLoad() {
        final ChessGame chessGame = defaultChessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, e7, e5, null)
        );

        // INVALID. Valid players turn but invalid pieces usage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e7, e5, null)
        );

        // INVALID. Piece not exists.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e3, e4, null)
        );

        // INVALID. Invalid Pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e2, e5, null)
        );

        // INVALID. Invalid Pawn move, can`t capture void.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e2, d3, null)
        );

        // VALID. First valid move, pawn passage.
        chessGame.makeMovement(firstPlayerUsername, e2, e4, null);

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c8, f5, null)
        );

        // INVALID. Valid players turn, but invalid figures turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c1, f4, null)
        );

        // INVALID. Bishop can`t move when path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, c8, f5, null)
        );

        // VALID. Valid pawn passage.
        chessGame.makeMovement(secondPlayerUsername, e7, e5, null);

        // INVALID. King can`t long castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e1, c1, null)
        );

        // INVALID. King can`t short castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e1, g1, null)
        );

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, a1, a5, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b1, b3, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(firstPlayerUsername, b1, c3, null);

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, a8, a4, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b8, b6, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(secondPlayerUsername, g8, f6, null);

        // INVALID. Invalid pawn passage, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, c3, null)
        );

        // INVALID. Invalid pawn move, end field is not.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, c3, null)
        );

        // INVALID. Invalid pawn capture operation, nothing to capture and it is not a capture on passage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, c2, d3, null)
        );

        // INVALID. Invalid pawn move, can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e4, e3, null)
        );

        // INVALID. Invalid pawn move, can`t move diagonal-back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, e4, f3, null)
        );

        // INVALID. Invalid Queen move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, d1, e3, null)
        );

        // VALID. Knight move.
        chessGame.makeMovement(firstPlayerUsername, g1, f3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, b8, c6, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, f1, b5, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, a7, a6, null);

        // INVALID. Invalid Bishop move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, b5, d5, null)
        );

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, d2, d3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, d7, d6, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(firstPlayerUsername, b5, c6, null);

        // INVALID. Valid Bishop move but it`s not safety for King.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, f8, e7, null)
        );

        // VALID. Pawn captures enemy bishop threatening King.
        chessGame.makeMovement(secondPlayerUsername, b7, c6, null);

        // VALID. Short castle.
        chessGame.makeMovement(firstPlayerUsername, e1, g1, null);

        // INVALID. Invalid rook move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, a8, b6, null)
        );

        // INVALID. Invalid pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, c6, c4, null)
        );

        // INVALID. Valid Knight move but end field occupied by same color piece.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, f6, h7, null)
        );

        // INVALID. Invalid King move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, e8, e6, null)
        );

        // INVALID. Invalid pawn movement, pawn can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.g7, Coordinate.g8, null)
        );

        // INVALID. Invalid Knight move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.f6, Coordinate.f5, null)
        );

        // INVALID. Invalid diagonal pawn movement distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.d3, Coordinate.e4, null)
        );

        // VALID. Bishop move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.c8, Coordinate.g4, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.h2, Coordinate.h3, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.g4, Coordinate.f3, null);

        // VALID. Queen capture Bishop.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.d1, Coordinate.f3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.f6, Coordinate.d7, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.c1, Coordinate.e3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.f7, Coordinate.f6, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.a2, Coordinate.a3, null);

        // VALID, Rook move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.a8, Coordinate.b8, null);
    }

    static Supplier<ChessGame> defaultChessGameSupplier() {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer").get(),
                userAccountSupplier("secondPlayer").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT, false);
    }

    static Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> UserAccount.of(new Username(username), new Email("some@email.com"), new Password("password"));
    }
}
