package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.PersonalData;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

@Disabled("Just utility")
class QuickTests {
    private final ChessGame chessGame = chessGameSupplier().get();
    private final ChessBoardNavigator navigator = new ChessBoardNavigator(chessGame.getChessBoard());
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername();

    @Test
    void test() {
        // 1. e2-e3 a7-a5 2. Qd1-h5 Ra8-a6 3. Qh5xa5 h7-h5 4. Qa5xc7 Ra6-h6 5. h2-h4 f7-f6 6. Qc7xd7+ ...

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.e2, Coordinate.e3, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.a7, Coordinate.a5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.d1, Coordinate.h5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.a8, Coordinate.a6, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.h5, Coordinate.a5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.h7, Coordinate.h5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.a5, Coordinate.c7, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.a6, Coordinate.h6, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.h2, Coordinate.h4, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.f7, Coordinate.f6, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.c7, Coordinate.d7, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.e8, Coordinate.f7, null);
        Log.info(navigator.prettyToString());
    }

    private void returnMove() {
        chessGame.returnMovement(usernameOfPlayerForWhites);
        chessGame.returnMovement(usernameOfPlayerForBlacks);
        Log.info(navigator.prettyToString());
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

    static Supplier<ChessGame> chessGameSupplier(String fen) {
        final ChessBoard chessBoard = ChessBoard.pureChessFromPosition(fen);

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