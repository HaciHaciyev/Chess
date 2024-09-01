package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.Direction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static core.project.chess.domain.aggregates.chess.enumerations.Coordinate.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChessGameTest {

    @Test /** In this method, a full-fledged game is played with its logic, and both valid and invalid moves are present.*/
    @Disabled("For single performance checks.")
    void testChessGamePerformance() {

        for (int i = 0; i < 150_000; i++) {

            long startTime = System.nanoTime();

            chessGameLoad();

            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            System.out.println(duration);
        }

    }

    @Test
    @DisplayName("Test chess game with many invalid moves.")
    void testChessGame() {
        chessGameLoad();
    }

    @Test
    @DisplayName("Chess game end by pat in 10 move.")
    void testChessGameEndByPat() {
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        // 1.
        chessGame.makeMovement(firstPlayerUsername, E2, E3, null);

        chessGame.makeMovement(secondPlayerUsername, A7, A5, null);

        // 2.
        chessGame.makeMovement(firstPlayerUsername, D1, H5, null);

        chessGame.makeMovement(secondPlayerUsername, A8, A6, null);

        // 3.
        chessGame.makeMovement(firstPlayerUsername, H5, A5, null);

        chessGame.makeMovement(secondPlayerUsername, H7, H5, null);

        // 4.
        chessGame.makeMovement(firstPlayerUsername, A5, C7, null);

        chessGame.makeMovement(secondPlayerUsername, A6, H6, null);

        // 5.
        chessGame.makeMovement(firstPlayerUsername, H2, H4, null);

        chessGame.makeMovement(secondPlayerUsername, F7, F6, null);

        // 6.
        chessGame.makeMovement(firstPlayerUsername, C7, D7, null);

        chessGame.makeMovement(secondPlayerUsername, E8, F7, null);

        // 7.
        chessGame.makeMovement(firstPlayerUsername, D7, B7, null);

        chessGame.makeMovement(secondPlayerUsername, D8, D3, null);

        // 8.
        chessGame.makeMovement(firstPlayerUsername, B7, B8, null);

        chessGame.makeMovement(secondPlayerUsername, D3, H7, null);

        // 9.
        chessGame.makeMovement(firstPlayerUsername, B8, C8, null);

        chessGame.makeMovement(secondPlayerUsername, F7, G6, null);

        // 10. STALEMATE
        chessGame.makeMovement(firstPlayerUsername, C8, E6, null);

        assertEquals(GameResult.DRAW, chessGame.gameResult().orElseThrow());
    }

    @Test
    @DisplayName("Test directions path.")
    void testDirectionsPath() {
        ChessGame game = chessGameSupplier().get();

        List<ChessBoard.Field> coords = Direction.fieldsOfPathExclusive(game.getChessBoard(), D1, E2);
        List<ChessBoard.Field> coords1 = Direction.fieldsOfPathInclusive(game.getChessBoard(), E2, G6);

        System.out.println(coords);
        coords1.forEach(f -> System.out.println(f.pieceOptional()));
    }

    @Test
    void testNewDirectionFeat() {
        ChessGame game = chessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername().username();
        String blackPlayer = game.getPlayerForBlack().getUsername().username();

        //1.
        game.makeMovement(whitePlayer, E2, E4, null);

        game.makeMovement(blackPlayer, E7, E5, null);

        //2.
        game.makeMovement(whitePlayer, G1, F3, null);

        game.makeMovement(blackPlayer, B8, C6, null);

        //3.
        game.makeMovement(whitePlayer, B1, C3, null);

        game.makeMovement(blackPlayer, G8, F6, null);

        //4.
        game.makeMovement(whitePlayer, D2, D3, null);

        game.makeMovement(blackPlayer, F8, C5, null);

        //5.
        game.makeMovement(whitePlayer, C1, G5, null);

        game.makeMovement(blackPlayer, H7, H6, null);

        //6.
        game.makeMovement(whitePlayer, G5, F6, null);

        game.makeMovement(blackPlayer, D8, F6, null);

        //7.
        game.makeMovement(whitePlayer, A2, A3, null);

        game.makeMovement(blackPlayer, D7, D6, null);

        //8.
        game.makeMovement(whitePlayer, C3, D5, null);

        game.makeMovement(blackPlayer, F6, D8, null);

        //9.
        game.makeMovement(whitePlayer, C2, C3, null);

        game.makeMovement(blackPlayer, C8, E6, null);

        //10.
        game.makeMovement(whitePlayer, D1, A4, null);

        game.makeMovement(blackPlayer, D8, D7, null);

        //11.
        game.makeMovement(whitePlayer, D5, E3, null);

        game.makeMovement(blackPlayer, C5, E3, null);

        //12.
        game.makeMovement(whitePlayer, F2, E3, null);

        game.makeMovement(blackPlayer, E8, G8, null);

        //13.
        game.makeMovement(whitePlayer, D3, D4, null);

        game.makeMovement(blackPlayer, E6, G4, null);

        //14.
        game.makeMovement(whitePlayer, F3, D2, null);

        game.makeMovement(blackPlayer, D7, E7, null);

        //15.
        game.makeMovement(whitePlayer, D4, E5, null);

        game.makeMovement(blackPlayer, D6, E5, null);

        //16.
        game.makeMovement(whitePlayer, D2, C4, null);

        game.makeMovement(blackPlayer, E7, G5, null);

        //17.
        game.makeMovement(whitePlayer, F1, D3, null);

        game.makeMovement(blackPlayer, G4, E6, null);

        //18.
        game.makeMovement(whitePlayer, G2, G3, null);

        game.makeMovement(blackPlayer, E6, C4, null);


        List<ChessBoard.Field> fields = Direction.occupiedFieldsFromDiagonalDirections(
                game.getChessBoard(), C6, field -> field.isPresent() && !field.getCoordinate().equals(E4)
        );

        fields.forEach(f -> System.out.println("Piece: " + f.pieceOptional() + "\n" + "Coordinate: " + f.getCoordinate()));

    }


    @Test
    @DisplayName("ChessBoard FEN render test")
    void testFENRender() {
        System.out.println(ChessBoard.renderASCII("r2qk2r/ppp2pp1/2npb2p/2bNp3/4P3/P1PP1N2/1P3PPP/R2QKB1R w KQkq - 1 10"));
    }

    @Test
    @DisplayName("Test fastest stalemate")
    void fastestStalemate() {

        ChessGame game = chessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername().username();
        String blackPlayer = game.getPlayerForBlack().getUsername().username();

        game.makeMovement(whitePlayer, E2, E3, null);

        game.makeMovement(blackPlayer, A7, A5, null);


        game.makeMovement(whitePlayer, D1, H5, null);

        game.makeMovement(blackPlayer, A8, A6, null);


        game.makeMovement(whitePlayer, H5, A5, null);

        game.makeMovement(blackPlayer, H7, H5, null);


        game.makeMovement(whitePlayer, H2, H4, null);

        game.makeMovement(blackPlayer, A6, H6, null);


        game.makeMovement(whitePlayer, A5, C7, null);

        game.makeMovement(blackPlayer, F7, F6, null);


        game.makeMovement(whitePlayer, C7, D7, null);

        game.makeMovement(blackPlayer, E8, F7, null);


        game.makeMovement(whitePlayer, D7, B7, null);

        game.makeMovement(blackPlayer, D8, D3, null);


        game.makeMovement(whitePlayer, B7, B8, null);

        game.makeMovement(blackPlayer, D3, H7, null);


        game.makeMovement(whitePlayer, B8, C8, null);

        game.makeMovement(blackPlayer, F7, G6, null);


        game.makeMovement(whitePlayer, C8, E6, null);
    }

    @Test
    @DisplayName("Game between AinGrace and Hadzhy98 on 2024.08.04\nhttps://lichess.org/zuOBpEUY#11")
    void gameOn_2024_08_04() {
        ChessGame game = chessGameSupplier().get();

        String whitePlayer = game.getPlayerForWhite().getUsername().username();
        String blackPlayer = game.getPlayerForBlack().getUsername().username();

        //1.
        game.makeMovement(whitePlayer, E2, E4, null);

        game.makeMovement(blackPlayer, E7, E5, null);

        //2.
        game.makeMovement(whitePlayer, G1, F3, null);

        game.makeMovement(blackPlayer, B8, C6, null);

        //3.
        game.makeMovement(whitePlayer, B1, C3, null);

        game.makeMovement(blackPlayer, G8, F6, null);

        //4.
        game.makeMovement(whitePlayer, D2, D3, null);

        game.makeMovement(blackPlayer, F8, C5, null);

        //5.
        game.makeMovement(whitePlayer, C1, G5, null);

        game.makeMovement(blackPlayer, H7, H6, null);

        //6.
        game.makeMovement(whitePlayer, G5, F6, null);

        game.makeMovement(blackPlayer, D8, F6, null);

        //7.
        game.makeMovement(whitePlayer, A2, A3, null);

        game.makeMovement(blackPlayer, D7, D6, null);

        //8.
        game.makeMovement(whitePlayer, C3, D5, null);

        game.makeMovement(blackPlayer, F6, D8, null);

        //9.
        game.makeMovement(whitePlayer, C2, C3, null);

        game.makeMovement(blackPlayer, C8, E6, null);

        //10.
        game.makeMovement(whitePlayer, D1, A4, null);

        game.makeMovement(blackPlayer, D8, D7, null);

        //11.
        game.makeMovement(whitePlayer, D5, E3, null);

        game.makeMovement(blackPlayer, C5, E3, null);

        //12.
        game.makeMovement(whitePlayer, F2, E3, null);

        game.makeMovement(blackPlayer, E8, G8, null);

        //13.
        game.makeMovement(whitePlayer, D3, D4, null);

        game.makeMovement(blackPlayer, E6, G4, null);

        //14.
        game.makeMovement(whitePlayer, F3, D2, null);

        game.makeMovement(blackPlayer, D7, E7, null);

        //15.
        game.makeMovement(whitePlayer, D4, E5, null);

        game.makeMovement(blackPlayer, D6, E5, null);

        //16.
        game.makeMovement(whitePlayer, D2, C4, null);

        game.makeMovement(blackPlayer, E7, G5, null);

        //17.
        game.makeMovement(whitePlayer, F1, D3, null);

        game.makeMovement(blackPlayer, G4, E6, null);

        //18.
        game.makeMovement(whitePlayer, G2, G3, null);

        game.makeMovement(blackPlayer, E6, C4, null);

        //19.
        game.makeMovement(whitePlayer, D3, C4, null);

        game.makeMovement(blackPlayer, G5, E3, null);

        //20. fixed
        game.makeMovement(whitePlayer, C4, E2, null);

        game.makeMovement(blackPlayer, A7, A6, null);

        //21.
        game.makeMovement(whitePlayer, H1, F1, null);

        game.makeMovement(blackPlayer, B7, B5, null);

        //22.
        game.makeMovement(whitePlayer, A4, C2, null);

        game.makeMovement(blackPlayer, A6, A5, null);

        //23.
        game.makeMovement(whitePlayer, C3, C4, null);

        game.makeMovement(blackPlayer, B5, B4, null);

        //24.
        game.makeMovement(whitePlayer, A3, B4, null);

        game.makeMovement(blackPlayer, C6, B4, null);

        //25.
        game.makeMovement(whitePlayer, C2, B1, null);

        game.makeMovement(blackPlayer, F8, D8, null);

        //26.
        game.makeMovement(whitePlayer, F1, F5, null);

        game.makeMovement(blackPlayer, F7, F6, null);

        //27.
        game.makeMovement(whitePlayer, A1, A3, null);

        game.makeMovement(blackPlayer, E3, D2, null);

        //28.
        game.makeMovement(whitePlayer, E1, F1, null);

        game.makeMovement(blackPlayer, D2, C2, null);

        //29.
        game.makeMovement(whitePlayer, A3, A1, null);

        game.makeMovement(blackPlayer, D8, D4, null);

        //30.
        game.makeMovement(whitePlayer, E2, F3, null);

        game.makeMovement(blackPlayer, C2, C4, null);

        //31.
        game.makeMovement(whitePlayer, F1, G2, null);

        game.makeMovement(blackPlayer, D4, D2, null);

        //32.
        game.makeMovement(whitePlayer, G2, G1, null);

        game.makeMovement(blackPlayer, G8, F7, null);

        //33.
        game.makeMovement(whitePlayer, B1, F1, null);

        game.makeMovement(blackPlayer, C4, F1, null);

        //34.
        game.makeMovement(whitePlayer, G1, F1, null);

        game.makeMovement(blackPlayer, G7, G6, null);

        //35.
        game.makeMovement(whitePlayer, F1, E1, null);

        game.makeMovement(blackPlayer, D2, B2, null);

        //36.
        game.makeMovement(whitePlayer, F3, H5, null);

        game.makeMovement(blackPlayer, B4, C2, null);

        //37.
        game.makeMovement(whitePlayer, E1, F2, null);

        game.makeMovement(blackPlayer, C2, A1, null);

        //38.
        game.makeMovement(whitePlayer, F2, F3, null);

        game.makeMovement(blackPlayer, A8, D8, null);

        //39.
        game.makeMovement(whitePlayer, F3, G4, null);

        game.makeMovement(blackPlayer, C7, C5, null);

        //40.
        game.makeMovement(whitePlayer, H2, H4, null);

        game.makeMovement(blackPlayer, C5, C4, null);

        //41.
        game.makeMovement(whitePlayer, G4, F3, null);

        game.makeMovement(blackPlayer, C4, C3, null);

        //42. checkmate
        game.makeMovement(whitePlayer, G3, G4, null);

        game.makeMovement(blackPlayer, D8, D3, null);
    }

    public void chessGameLoad() {
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, E7, E5, null)
        );

        // INVALID. Valid players turn but invalid pieces usage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E7, E5, null)
        );

        // INVALID. Piece not exists.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E3, E4, null)
        );

        // INVALID. Invalid Pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E2, E5, null)
        );

        // INVALID. Invalid Pawn move, can`t capture void.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E2, D3, null)
        );

        // VALID. First valid move, pawn passage.
        chessGame.makeMovement(firstPlayerUsername, E2, E4, null);

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, C8, F5, null)
        );

        // INVALID. Valid players turn, but invalid figures turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, C1, F4, null)
        );

        // INVALID. Bishop can`t move when path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, C8, F5, null)
        );

        // VALID. Valid pawn passage.
        chessGame.makeMovement(secondPlayerUsername, E7, E5, null);

        // INVALID. King can`t long castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E1, C1, null)
        );

        // INVALID. King can`t short castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E1, G1, null)
        );

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, A1, A5, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, B1, B3, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(firstPlayerUsername, B1, C3, null);

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, A8, A4, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, B8, B6, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(secondPlayerUsername, G8, F6, null);

        // INVALID. Invalid pawn passage, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, C2, C3, null)
        );

        // INVALID. Invalid pawn move, end field is not.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, C2, C3, null)
        );

        // INVALID. Invalid pawn capture operation, nothing to capture and it is not a capture on passage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, C2, D3, null)
        );

        // INVALID. Invalid pawn move, can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E4, E3, null)
        );

        // INVALID. Invalid pawn move, can`t move diagonal-back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, E4, F3, null)
        );

        // INVALID. Invalid Queen move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, D1, E3, null)
        );

        // VALID. Knight move.
        chessGame.makeMovement(firstPlayerUsername, G1, F3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, B8, C6, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, F1, B5, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, A7, A6, null);

        // INVALID. Invalid Bishop move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, B5, D5, null)
        );

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, D2, D3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, D7, D6, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(firstPlayerUsername, B5, C6, null);

        // INVALID. Valid Bishop move but it`s not safety for King.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, F8, E7, null)
        );

        // VALID. Pawn captures enemy bishop threatening King.
        chessGame.makeMovement(secondPlayerUsername, B7, C6, null);

        // VALID. Short castle.
        chessGame.makeMovement(firstPlayerUsername, E1, G1, null);

        // INVALID. Invalid rook move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, A8, B6, null)
        );

        // INVALID. Invalid pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, C6, C4, null)
        );

        // INVALID. Valid Knight move but end field occupied by same color piece.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, F6, H7, null)
        );

        // INVALID. Invalid King move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, E8, E6, null)
        );

        // INVALID. Invalid pawn movement, pawn can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.G7, Coordinate.G8, null)
        );

        // INVALID. Invalid Knight move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.F6, Coordinate.F5, null)
        );

        // INVALID. Invalid diagonal pawn movement distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.D3, Coordinate.E4, null)
        );

        // VALID. Bishop move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.C8, Coordinate.G4, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.H2, Coordinate.H3, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.G4, Coordinate.F3, null);

        // VALID. Queen capture Bishop.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.D1, Coordinate.F3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.F6, Coordinate.D7, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.C1, Coordinate.E3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.F7, Coordinate.F6, null);

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.A2, Coordinate.A3, null);

        // VALID, Rook move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.A8, Coordinate.B8, null);
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
