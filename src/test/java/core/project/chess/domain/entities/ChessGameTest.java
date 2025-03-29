package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Queen;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.ChessMove;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.PersonalData;
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

@Disabled("It is more of a performance test and was disabled due to the long execution time, but the test itself is completely passed.")
public class ChessGameTest {

    @Test
    void pgnUtil() {
        final String pgn = """
                1. e2-e4 c7-c6 2. Ng1-f3 d7-d5 3. e4xd5 c6xd5 4. d2-d4 Nb8-c6 5. Nb1-c3 e7-e6 6. Bf1-e2 Bf8-d6 7. O-O Ng8-f6 8. Bc1-g5 O-O 9. Qd1-d2 Bd6-e7 10. Rf1-e1 Qd8-c7 11. Bg5-f4 Qc7-d7 12. Be2-b5 a7-a6 13. Bb5-a4 b7-b5 14. Ba4-b3 Qd7-a7 15. Nf3-h4 Qa7-d7 16. Bf4-h6 g7xh6 17. Qd2xh6 Nf6-e4 18. Nc3xe4 d5xe4 19. Re1xe4 f7-f5 20. Re4xe6 Kg8-h8 21. Nh4xf5 Rf8xf5 22. Ra1-e1 Be7-f8 23. Qh6-h3 Rf5-g5 24. Re1-e4 Qd7-g7 25. g2-g3 Bc8xe6 26. Bb3xe6 Ra8-e8 27. d4-d5 Nc6-e5 28. f2-f4 Ne5-f3+ 29. Kg1-h1 Rg5-g6 30. f4-f5 Rg6-g5 31. Kh1-g2 Nf3-d4 32. c2-c3 Nd4xe6 33. d5xe6 Bf8-d6 34. g3-g4 Qg7-f6 35. Qh3-d3 Bd6-b8 36. Qd3-d7 Qf6-h6 37. e6-e7 Qh6xh2+ 38. Kg2-f3 Qh2-g3+ 39. Kf3-e2 Qg3-g2+ 40. Ke2-d1 Qg2xe4 41. Qd7xe8+ Rg5-g8 42. Qe8-f7 Qe4xg4+ 43. Kd1-c2 Qg4-g2+ 44. Kc2-b3 a6-a5 45. e7-e8=Q a5-a4+ 46. Kb3-a3 Bb8-d6+ 47. b2-b4 a4xb3+ 48. Ka3xb3 Rg8xe8 49. Qf7xe8+ Kh8-g7 50. Qe8-d7+ Kg7-h6 51. Qd7xd6+ Kh6-g5 52. f5-f6 Qg2-f3 53. Qd6-e5+ Kg5-g6 54. Qe5xb5 Qf3xf6 55. a2-a4 Qf6-e6+ 56. Kb3-a3 Qe6-d6+ 57. Qb5-b4 Qd6-d3 58. a4-a5 h7-h5 59. Qb4-b6+ Kg6-g5 60. Qb6-c5+ Kg5-g4 61. Ka3-b4 Qd3-b1+ 62. Kb4-c4 Qb1-a2+ 63. Kc4-d3 Qa2-b1+ 64. Kd3-c4 Qb1-a2+ 65. Kc4-d4 Qa2-d2+ 66. Kd4-c4 Qd2-a2+ 67. Kc4-b5 Qa2-b3+ 68. Qc5-b4+ Qb3xb4+ 69. Kb5xb4 h5-h4 70. a5-a6 h4-h3 71. a6-a7 h3-h2 72. a7-a8=Q h2-h1=B 73. Qa8xh1 Kg4-f4 74. c3-c4 Kf4-g3 75. Qh1-e1+ Kg3-f3 76. Qe1-d2 Kf3-e4
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
        ChessGame game = chessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername();
        String black = game.getPlayerForBlack().getUsername();

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
        ChessGame game = chessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername();
        String black = game.getPlayerForBlack().getUsername();

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
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername();

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
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername();

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
        ChessGame game = chessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername();
        String blackPlayer = game.getPlayerForBlack().getUsername();

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
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername();

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

    @Test
    void revertPromotion() {
        ChessGame game = chessGameSupplier().get();
        ChessBoardNavigator navigator = new ChessBoardNavigator(game.getChessBoard());

        String whitePlayer = game.getPlayerForWhite().getUsername();
        String blackPlayer = game.getPlayerForBlack().getUsername();

        //1.
        game.makeMovement(whitePlayer, e2, e4, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, e7, e5, null);
        System.out.println(navigator.prettyToString());

        //2.
        game.makeMovement(whitePlayer, f2, f4, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, e5, f4, null);
        System.out.println(navigator.prettyToString());

        //3.
        game.makeMovement(whitePlayer, g2, g3, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, f4, g3, null);
        System.out.println(navigator.prettyToString());

        //4.
        game.makeMovement(whitePlayer, g1, f3, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, g3, h2, null);
        System.out.println(navigator.prettyToString());

        //5.
        game.makeMovement(whitePlayer, f3, g1, null);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, h2, g1, new Queen(Color.BLACK));
        System.out.println(navigator.prettyToString());

        game.returnMovement(whitePlayer);
        game.returnMovement(blackPlayer);
        System.out.println(navigator.prettyToString());

        game.makeMovement(blackPlayer, d8, h4, new Queen(Color.BLACK));
        System.out.println(navigator.prettyToString());
    }

    static Supplier<ChessGame> chessGameSupplier() {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer").get(),
                userAccountSupplier("secondPlayer").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT, false);
    }

    public static Supplier<ChessGame> chessGameSupplier(String FEN) {
        final ChessBoard chessBoard = ChessBoard.fromPosition(FEN);

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer").get(),
                userAccountSupplier("secondPlayer").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT, false);
    }

    static Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> UserAccount.of(new PersonalData(
                "generateFirstname",
                "generateSurname",
                username,
                "some@email.com",
                "password"
        ));
    }
}
