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
    private final ChessGame chessGame = chessGameSupplier("rnbqkbnr/1ppppppp/8/8/Pp6/8/2PPPPPP/RNBQKBNR b KQkq a3 0 1").get();
    private final ChessBoardNavigator navigator = new ChessBoardNavigator(chessGame.getChessBoard());
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername();

    @Test
    void perft() {
        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.b4, Coordinate.a3, null);
        Log.info(navigator.prettyToString());
/*
        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.e7, Coordinate.e5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.d1, Coordinate.h5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.e8, Coordinate.e7, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.h5, Coordinate.e5, null);
        Log.info(navigator.prettyToString());*/
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