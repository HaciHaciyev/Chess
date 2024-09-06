package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.ChessMove;
import core.project.chess.infrastructure.utilities.SimplePGNReader;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static core.project.chess.domain.aggregates.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChessGameTest {

    private static final Logger log = LoggerFactory.getLogger(ChessGameTest.class);

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
        final ChessGame chessGame = chessGameSupplier().get();

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
    @DisplayName("game from pgn")
    void gameFromPGNTest() {
        ChessGame game = chessGameSupplier().get();

        String white = game.getPlayerForWhite().getUsername().username();
        String black = game.getPlayerForBlack().getUsername().username();

        for (String pgn : extractPGN()) {
            SimplePGNReader pgnReader = new SimplePGNReader(pgn);
            List<ChessMove> moves = pgnReader.readAll();

            for (ChessMove move : moves) {

                Log.info(move.white());

                game.makeMovement(white, move.white().from(), move.white().to(), null);

                Log.info("White : " + game.getChessBoard().lastAlgebraicNotation().algebraicNotation());
                Log.info(game.getChessBoard().actualRepresentationOfChessBoard());
                Log.info(game.getChessBoard().pgn());

                System.out.println();

                Log.info(move.black());

                game.makeMovement(black, move.black().from(), move.black().to(), null);
                Log.info("Black : " + game.getChessBoard().lastAlgebraicNotation().algebraicNotation());
                Log.info(game.getChessBoard().actualRepresentationOfChessBoard());
                Log.info(game.getChessBoard().pgn());

                System.out.println();

            }
        }
    }

    private static String[] extractPGN() {
        File file = new File("src/main/resources/chess/pgn/Berliner_lalg.pgn");

        StringBuilder sb;
        String[] pgnArr;
        try (FileReader fileReader = new FileReader(file)) {
            sb = new StringBuilder();

            int i = 0;
            while (fileReader.ready()) {
                char ch = (char) fileReader.read();
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n' && ch == '\n') {
                    i++;
                    if (i % 2 == 0) {
                        sb.append("---");
                    }
                }

                sb.append(ch);
            }

            pgnArr = sb.toString().split("---");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pgnArr;
    }

    @Test
    @DisplayName("Chess game end by pat in 10 move.")
    void testChessGameEndByPat() {
        final ChessGame chessGame = chessGameSupplier().get();

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
        ChessGame game = chessGameSupplier().get();

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
        final ChessGame chessGame = chessGameSupplier().get();

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

    public final Supplier<ChessGame> chessGameSupplier() {
        return () -> {
            final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());

            return ChessGame.of(
                    UUID.randomUUID(),
                    chessBoard,
                    userAccountSupplier("firstPlayer").get(),
                    userAccountSupplier("secondPlayer").get(),
                    SessionEvents.defaultEvents(),
                    ChessGame.TimeControllingTYPE.DEFAULT
            );
        };
    }

    public final Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> UserAccount.of(new Username(username), new Email("some@email.com"), new Password("password"));
    }
}
