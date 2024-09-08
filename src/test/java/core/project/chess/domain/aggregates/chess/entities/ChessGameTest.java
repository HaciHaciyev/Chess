package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.chess.ChessMove;
import core.project.chess.infrastructure.utilities.chess.SimplePGNReader;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static core.project.chess.domain.aggregates.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChessGameTest {


    @Test
    @Disabled("Utility function.")
    void removeDashes() {
        final String pgn = """
                1. e2-e4 c7-c5 2. Ng1-f3 d7-d6 3. Bf1-b5+ Nb8-d7 4. d2-d4 c5xd4 5. Qd1xd4 Qd8-c7 6. Nb1-c3 e7-e6 7. O-O Ng8-e7 8. Bb5xd7+ Bc8xd7 
                9. Rf1-d1 Ne7-g6 10. Bc1-e3 a7-a6 11. a2-a4 Ra8-c8 12. a4-a5 Ng6-e5 13. Nf3xe5 d6xe5 14. Qd4-d3 Bd7-c6 15. Be3-b6 Qc7-e7 16. Ra1-b1 Qe7-b4 
                17. Qd3-h3 Bf8-d6 18. Qh3-e3 Bd6-e7 19. f2-f3 O-O 20. Kg1-h1 Bc6-b5 21. Nc3xb5 Qb4xb5 22. c2-c3 Be7-c5 23. Bb6xc5 Rc8xc5 24. b2-b4 Rc5-c7 
                25. h2-h3 Rf8-c8 26. Rd1-d3 h7-h6 27. Rb1-d1 Kg8-h7 28. Qe3-d2 Rc7-c4 29. Rd3-d7 Rc8-c7 30. Qd2-d6 Qb5-a4 31. Rd7xc7 Rc4xc7 32. Qd6xc7 Qa4xd1+ 
                33. Kh1-h2 Qd1-d2 34. Qc7xf7 Qd2xc3 35. Qf7xb7 Qc3-d2 36. Qb7-b6 Qd2-f4+ 37. Kh2-h1 Qf4-c1+ 38. Qb6-g1 Qc1-b2 39. Qg1-c5 h6-h5 40. Kh1-h2 h5-h4 
                41. Qc5-d6 Qb2-b3 42. Qd6xa6 Qb3xb4 43. Qa6-b6 Qb4-e1 44. a5-a6 Qe1-g3+ 45. Kh2-h1 Qg3-e1+ 46. Qb6-g1 Qe1-a5 47. a6-a7 g7-g5 48. Kh1-h2 Kh7-g6 
                49. Qg1-f2 Kg6-h6 50. Qf2-e3 Kh6-h5 51. Qe3-g1 Kh5-h6 52. Qg1-f2 Kh6-g6 53. Qf2-e3 Kg6-h5 54. Qe3-b3 Qa5xa7 55. Qb3xe6 Qa7-c5 
                56. Qe6-f7+ Kh5-h6 57. Qf7-f6+ Kh6-h5 58. Qf6-g7 Qc5-d6 59. g2-g4#
                """;

        String result = pgn.replaceAll("-", "");
        Log.info(result);
    }

    @Test /** In this method, a full-fledged game is played with its logic, and both valid and invalid moves are present.*/
    @Disabled("For single performance checks.")
    void testChessGamePerformance() {

        long startTime = System.nanoTime();

        for (int i = 0; i < 150_000; i++) {

            chessGameLoad();
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println(duration);
    }

    @Test
    @DisplayName("Test chess game with many invalid moves.")
    void testChessGame() {
        chessGameLoad();
    }

    @Test
    @DisplayName("Simple testing of FEN.")
    void fenTest() {
        final ChessGame chessGame = defaultChessGameFactory();

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
    @DisplayName("Berliner_PGN_Archive_64")
    void berliner64() {
        executeGameFromPGN("src/main/resources/chess/pgn/Berliner_lalg.pgn");
    }

    @Test
    @DisplayName("Mamedyarov_PGN_Archive_4684")
    void mamedyarov4684() {
        executeGameFromPGN("src/main/resources/chess/pgn/Mamedyarov_lalg.pgn");
    }

    @Test
    @DisplayName("Hikaru_PGN_Archive_8025")
    void nakamura8025() {
        executeGameFromPGN("src/main/resources/chess/pgn/Hikaru_lalg.pgn");
    }

    private void executeGameFromPGN(String path) {
        int pgnNum = 0;
        for (String pgn : extractPGN(path)) {
            pgnNum++;
            ChessGame game = defaultChessGameFactory();

            String white = game.getPlayerForWhite().getUsername().username();
            String black = game.getPlayerForBlack().getUsername().username();

            SimplePGNReader pgnReader = new SimplePGNReader(pgn);
            List<ChessMove> moves = pgnReader.readAll();

            /*Log.info("""
                    Simulating the game of:
                    %s
                    """.formatted(pgn));*/

            int moveNum = 0;
            for (ChessMove move : moves) {
                if (move.white() == null) {
                    break;
                }

                Log.info("Move#" + ++moveNum + " | " + "Game#" + pgnNum);

                if (game.getChessBoard().lastAlgebraicNotation().isPresent()) {
                    //Log.info("Last algebraic notation before white moving : " + game.getChessBoard().lastAlgebraicNotation().orElseThrow().algebraicNotation());
                }

                game.makeMovement(white, move.white().from(), move.white().to(), move.white().promotion());

                if (move.black() == null) {
                    break;
                }

                //Log.info("Last algebraic notation before black moving : " + game.getChessBoard().lastAlgebraicNotation().orElseThrow().algebraicNotation());

                game.makeMovement(black, move.black().from(), move.black().to(), move.black().promotion());
            }

//            Log.info("Result: " + pgnReader.tag("Result"));
//            Log.info("Game status: " + (game.gameResult().isEmpty() ? "EMPTY_STATUS" : game.gameResult().orElseThrow()));
//            System.out.println();
        }
    }

    @Test
    @DisplayName("Mamedyarov_4674")
    void mamedyarov_4674() {
        String pgn = """
                [Event "Titled Tue 4th Jun Early"]
                [Site "chess.com INT"]
                [Date "2024.06.04"]
                [Round "10"]
                [White "Mamedyarov,S"]
                [Black "Bharath,Subramaniyam H"]
                [Result "1/2-1/2"]
                [WhiteElo "2734"]
                [BlackElo "2550"]
                [ECO "A45"]
                
                1. d2d4 g8f6 2. c1g5 f6e4 3. g5f4 c7c5 4. f2f3 d8a5+ 5. c2c3 e4f6 6. d4d5
                e7e6 7. e2e4 e6d5 8. e4d5 d7d6 9. b1d2 f8e7 10. c3c4 e8g8 11. f1d3 b7b5 12.
                g1e2 b5c4 13. d3c4 b8d7 14. e1g1 d7b6 15. e2c3 c8a6 16. c4a6 a5a6 17. d2e4
                a8d8 18. f1e1 a6b7 19. d1b3 f6d5 20. c3d5 b7d5 21. a1d1 d5c6 22. e4g3 e7f6
                23. g3f5 d6d5 24. g2g4 c5c4 25. b3c2 d5d4 26. g4g5 c6f3 27. g5f6 f3f4 28.
                e1f1 f4g5+ 29. g1h1 g5f6 30. c2g2 g8h8 31. f5d4 f6g6 32. g2g6 h7g6 33. d4c6
                d8d1 34. f1d1 b6a4 35. b2b3 a4b2 36. d1d2 c4c3 37. d2c2 f8c8 38. c6a7 c8a8
                39. a7b5 a8a2 40. c2c3 b2d1 41. c3c8+ h8h7 42. b5d6 f7f6 43. h2h4 d1f2+ 44.
                h1g1 f2h3+ 45. g1h1 g6g5 46. h4g5 h3g5 47. c8c3 a2d2 48. d6c4 d2d1+ 49.
                h1g2 h7g6 50. b3b4 d1b1 51. c4d6 b1b4 52. c3c7 b4b2+ 53. g2f1 b2d2 54. d6e8
                g5f7 55. c7e7 d2d8 56. f1f2 f6f5 57. f2g2 d8d3 58. e7e6+ g6h7 59. e6e7 h7g8
                60. e7a7 d3e3 61. e8c7 f7g5 62. c7d5 e3e4 63. d5e7+ g8h7 64. e7f5 e4g4+ 65.
                g2h2 g5f3+ 66. h2h3 g4g5 67. a7g7+ g5g7 68. f5g7 1/2-1/2
                """;

        ChessGame game = defaultChessGameFactory();

        String white = game.getPlayerForWhite().getUsername().username();
        String black = game.getPlayerForBlack().getUsername().username();

        SimplePGNReader pgnReader = new SimplePGNReader(pgn);
        List<ChessMove> moves = pgnReader.readAll();

        Log.info("""
                    Simulating the game of:
                    %s
                    """.formatted(pgn));
        int moveNum = 0;
        for (ChessMove move : moves) {
            if (move.white() == null) {
                break;
            }

            game.makeMovement(white, move.white().from(), move.white().to(), move.white().promotion());

            if (move.black() == null) {
                break;
            }

            game.makeMovement(black, move.black().from(), move.black().to(), move.black().promotion());
        }
    }

    private static List<String> extractPGN(String path) {
        File file = new File(path);
        List<String> pgnList = new ArrayList<>();

        try (var reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            int emptyLineOccurence = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) emptyLineOccurence++;

                /* tags and moves in PGN are separated by new line character
                    as in: [Tag value]
                           [AnotherTag value]
                           \n
                           1. e4 e5 etc.

                   and PGNs themselves are also separated by new line
                   so it means that every second new line is separating not tags and moves but 2 PGNs
                 */
                if (!sb.isEmpty() && emptyLineOccurence == 2) {
                    emptyLineOccurence = 0;
                    pgnList.add(sb.toString());
                    sb.delete(0, sb.length());
                    continue;
                }

                sb.append(line).append("\n");
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pgnList;
    }

    @Test
    @DisplayName("Chess game end by pat in 10 move.")
    void testChessGameEndByPat() {
        final ChessGame chessGame = defaultChessGameFactory();

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
        ChessGame game = defaultChessGameFactory();

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
        final ChessGame chessGame = defaultChessGameFactory();

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

    public final ChessGame defaultChessGameFactory() {
            final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());

        return ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier("firstPlayer").get(),
                userAccountSupplier("secondPlayer").get(),
                SessionEvents.defaultEvents(),
                ChessGame.TimeControllingTYPE.DEFAULT);
    }

    public final Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> UserAccount.of(new Username(username), new Email("some@email.com"), new Password("password"));
    }
}
