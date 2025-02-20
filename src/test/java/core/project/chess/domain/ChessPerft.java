package core.project.chess.domain;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.value_objects.PlayerMove;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.Password;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.logging.Log;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPerft {

    private final ChessGame chessGame = chessGameSupplier().get();
    private final PerftValues perftValues = PerftValues.newInstance();

    @Test
    void performanceTest() {
        perft(1);

        //assertEquals(2_439_530_234_167L, perftValues.nodes, "Nodes count mismatch");
        //Log.infof("Nodes count: %d", perftValues.nodes);
        //
        //assertEquals(125_208_536_153L, perftValues.captures, "Captures count mismatch");
        //Log.infof("Captures count: %d", perftValues.captures);
        //
        //assertEquals(319_496_827L, perftValues.capturesOnPassage, "En Passant captures count mismatch");
        //Log.infof("En Passant captures count: %d", perftValues.promotions);
        //
        //assertEquals(1_784_356_000L, perftValues.castles, "Castles count mismatch");
        //Log.infof("Castles count: %d", perftValues.castles);
        //
        //assertEquals(17_334_376L, perftValues.promotions, "Promotions count mismatch");
        //Log.infof("Promotions count: %d", perftValues.promotions);
        //
        //assertEquals(36_095_901_903L, perftValues.checks, "Checks count mismatch");
        //Log.infof("Checks count: %d", perftValues.checks);
        //
        //assertEquals(400_191_963L, perftValues.checkMates, "Checkmates count mismatch");
        //Log.infof("Checkmates count: %d", perftValues.checkMates);
    }

    void perft(int depth) {
        if (depth == 0) {
            perftValues.nodes++;
            return;
        }

        Set<PlayerMove> validMoves = chessGame.getChessBoard().generateValidMoves();
        Log.infof("Generated -> %s moves", validMoves.size());
    }

    private void processingOfTheMove(PlayerMove move) {

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class PerftValues {
        long nodes;
        long captures;
        long capturesOnPassage;
        long castles;
        long promotions;
        long checks;
        long checkMates;

        public static PerftValues newInstance() {
            return new PerftValues(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        public void accumulate(PerftValues other) {
            this.nodes += other.nodes;
            this.captures += other.captures;
            this.capturesOnPassage += other.capturesOnPassage;
            this.castles += other.castles;
            this.promotions += other.promotions;
            this.checks += other.checks;
            this.checkMates += other.checkMates;
        }
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
        return () -> UserAccount.of(new Username(username), new Email(email), new Password("password"));
    }
}
