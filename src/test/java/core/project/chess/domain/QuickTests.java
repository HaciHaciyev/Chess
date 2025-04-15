package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.Move;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.PersonalData;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Disabled("Just utility")
class QuickTests {
    private final ChessGame chessGame = chessGameSupplier().get();
    private final ToStringUtils navigator = new ToStringUtils(chessGame.getChessBoard());
    private final String usernameOfPlayerForWhites = chessGame.getPlayerForWhite().getUsername();
    private final String usernameOfPlayerForBlacks = chessGame.getPlayerForBlack().getUsername();

    @Test
    void test() {
        //  PGN: 1. c2-c4 f7-f5 2. Qd1-b3 Ke8-f7 3. c4-c5 ...
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.c2, Coordinate.c4, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.f7, Coordinate.f5, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.d1, Coordinate.b3, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForBlacks, Coordinate.e8, Coordinate.f7, null);
        Log.info(navigator.prettyToString());

        chessGame.makeMovement(usernameOfPlayerForWhites, Coordinate.c4, Coordinate.c5, null);
        Log.info(navigator.prettyToString());
    }

    @Test
    void moveGenerationTest() {
        ChessBoard chessBoard = ChessBoard.fromPosition("r3k2r/p1ppqpb1/b3pnp1/3PN3/1p2P3/2N2Q1p/PnPB1PPP/R2B1K1R w kq - 0 3");
        ToStringUtils toStringUtils = new ToStringUtils(chessBoard);
        logBoard(toStringUtils);

        List<Move> allValidMoves = chessBoard.generateAllValidMoves();
        System.out.println(allValidMoves);
        assertContains(allValidMoves, new Move(Coordinate.c3, Coordinate.b5, null));

        chessBoard.doMove(Coordinate.c3, Coordinate.b5);
        logBoard(toStringUtils);

        chessBoard.undoMove();
        logBoard(toStringUtils);

        List<Move> allValidMoves1 = chessBoard.generateAllValidMoves();
        System.out.println(allValidMoves1);
        assertContains(allValidMoves1, new Move(Coordinate.c3, Coordinate.b5, null));

        chessBoard.doMove(Coordinate.c3, Coordinate.b5);
        logBoard(toStringUtils);

        chessBoard.undoMove();
        logBoard(toStringUtils);

        chessBoard.doMove(Coordinate.c3, Coordinate.e2);
        logBoard(toStringUtils);
    }

    private static void logBoard(ToStringUtils toStringUtils) {
        System.out.println(toStringUtils.prettyToString());
    }

    private void returnMove() {
        chessGame.returnMovement(usernameOfPlayerForWhites);
        chessGame.returnMovement(usernameOfPlayerForBlacks);
        Log.info(navigator.prettyToString());
    }

    private static void assertContains(List<Move> allValidMoves, Object o) {
        if (!allValidMoves.contains(o))
            throw new AssertionError("Do not contains required data.");
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