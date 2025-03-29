package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.PersonalData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

class ChessBoardNavigatorTest {

    @Test
    void occupiedFieldsInDirections() {
        ChessGame chessGame = chessGameSupplier().get();
        List<Coordinate> coordinates = chessGame.getChessBoard()
                .navigator()
                .occupiedFieldsInDirections(Direction.allDirections(), Coordinate.e4);

        System.out.println(coordinates);
    }

    @Test
    void testOccupiedFieldsInDirections() {
    }

    @Test
    void testOccupiedFieldsInDirections1() {
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
        return () -> UserAccount.of(new PersonalData(
                "generateFirstname",
                "generateSurname",
                username,
                email,
                "password"
        ));
    }
}