package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChessGameTest {

    @Test /** In this method, a full-fledged game is played with its logic, and both valid and invalid moves are present.*/
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
    void testChessGame() {
        chessGameLoad();
    }

    public void chessGameLoad() {
        final ChessGame chessGame = chessGameSupplier().get();

        final String firstPlayerUsername = chessGame.getPlayerForWhite().getUsername().username();
        final String secondPlayerUsername = chessGame.getPlayerForBlack().getUsername().username();

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.E7, Coordinate.E5, null)
        );

        // INVALID. Valid players turn but invalid pieces usage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E7, Coordinate.E5, null)
        );

        // INVALID. Piece not exists.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E3, Coordinate.E4, null)
        );

        // INVALID. Invalid Pawn move distance.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E2, Coordinate.E5, null)
        );

        // INVALID. Invalid Pawn move, can`t capture void.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E2, Coordinate.D3, null)
        );

        // VALID. First valid move, pawn passage.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.E2, Coordinate.E4, null);

        // INVALID. Invalid players turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.C8, Coordinate.F5, null)
        );

        // INVALID. Valid players turn, but invalid figures turn.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.C1, Coordinate.F4, null)
        );

        // INVALID. Bishop can`t move when path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.C8, Coordinate.F5, null)
        );

        // VALID. Valid pawn passage.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.E7, Coordinate.E5, null);

        // INVALID. King can`t long castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E1, Coordinate.C1, null)
        );

        // INVALID. King can`t short castle, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E1, Coordinate.G1, null)
        );

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.A1, Coordinate.A5, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.B1, Coordinate.B3, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.B1, Coordinate.C3, null);

        // INVALID. Rook can`t move, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.A8, Coordinate.A4, null)
        );

        // INVALID. Invalid Knight movement.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.B8, Coordinate.B6, null)
        );

        // VALID. Valid Knight movement.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.G8, Coordinate.F6, null);

        // INVALID. Invalid pawn passage, path is not clear.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.C2, Coordinate.C3, null)
        );

        // INVALID. Invalid pawn move, end field is not.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.C2, Coordinate.C3, null)
        );

        // INVALID. Invalid pawn capture operation, nothing to capture and it is not a capture on passage.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.C2, Coordinate.D3, null)
        );

        // INVALID. Invalid pawn move, can`t move back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E4, Coordinate.E3, null)
        );

        // INVALID. Invalid pawn move, can`t move diagonal-back.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.E4, Coordinate.F3, null)
        );

        // INVALID. Invalid Queen move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.D1, Coordinate.E3, null)
        );

        // VALID. Knight move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.G1, Coordinate.F3, null);

        // VALID. Knight move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.B8, Coordinate.C6, null);

        // VALID. Bishop move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.F1, Coordinate.B5, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.A7, Coordinate.A6, null);

        // INVALID. Invalid Bishop move.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(firstPlayerUsername, Coordinate.B5, Coordinate.D5, null)
        );

        // VALID. Pawn move.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.D2, Coordinate.D3, null);

        // VALID. Pawn move.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.D7, Coordinate.D6, null);

        // VALID. Bishop capture Knight.
        chessGame.makeMovement(firstPlayerUsername, Coordinate.B5, Coordinate.C6, null);

        // INVALID. Valid Bishop move but it`s not safety for King.
        assertThrows(
                IllegalArgumentException.class,
                () -> chessGame.makeMovement(secondPlayerUsername, Coordinate.F8, Coordinate.E7, null)
        );

        // VALID. Pawn captures enemy bishop threatening King.
        chessGame.makeMovement(secondPlayerUsername, Coordinate.B7, Coordinate.C6, null);
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
