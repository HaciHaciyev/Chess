package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.util.ToStringUtils;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        String white = game.getWhitePlayer().getUsername();
        String black = game.getBlackPlayer().getUsername();

        ToStringUtils navigator = new ToStringUtils(game.getChessBoard());

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

        final String firstPlayerUsername = chessGame.getWhitePlayer().getUsername();
        final String secondPlayerUsername = chessGame.getBlackPlayer().getUsername();

        System.out.println(chessGame.getChessBoard().toString());

        // 1
        chessGame.makeMovement(firstPlayerUsername, e2, e4, null);

        System.out.println(chessGame.getChessBoard().toString());

        chessGame.makeMovement(secondPlayerUsername, e7, e5, null);

        System.out.println(chessGame.getChessBoard().toString());
    }

    public static Supplier<ChessGame> chessGameSupplier() {
        final ChessBoard chessBoard = ChessBoard.pureChess();

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer").get(),
                userAccountSupplier("secondPlayer").get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT, false);
    }

    public static Supplier<ChessGame> chessGameSupplier(String FEN) {
        final ChessBoard chessBoard = ChessBoard.pureChessFromPosition(FEN);

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
