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
    void perft() {
        // 1. a2-a3 a7-a5 2. f2-f3 e7-e5 3. g2-g4 Qd8-h4+
        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.a2, Coordinate.a3, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.a7, Coordinate.a5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.f2, Coordinate.f3, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.e7, Coordinate.e5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.g2, Coordinate.g4, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.d8, Coordinate.h4, null);
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