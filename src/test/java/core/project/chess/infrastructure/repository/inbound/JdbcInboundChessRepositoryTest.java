package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.repository.outbound.JdbcOutboundUserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
@Transactional
class JdbcInboundChessRepositoryTest {

    @Inject
    JdbcOutboundUserRepository userRepository;

    @Inject
    JdbcInboundChessRepository jdbcInboundChessRepository;

    @Test
    void test() {
        assertDoesNotThrow(() -> {
                final ChessGame chessGame = defaultChessGameSupplier("HHadzhy", "AinGrace").get();
                jdbcInboundChessRepository.completelySaveStartedChessGame(chessGame);

                final String firstUsername = chessGame.getPlayerForWhite().getUsername().username();
                final String secondUsername = chessGame.getPlayerForBlack().getUsername().username();

                chessGame.makeMovement(firstUsername, Coordinate.e2, Coordinate.e4, null);
                chessGame.makeMovement(secondUsername, Coordinate.e7, Coordinate.e5, null);

                chessGame.resignation(firstUsername);

                jdbcInboundChessRepository.completelyUpdateFinishedGame(chessGame);
        });
    }

    Supplier<ChessGame> defaultChessGameSupplier(final String first, final String second) {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier(first).get(),
                userAccountSupplier(second).get(),
                SessionEvents.defaultEvents(),
                ChessGame.TimeControllingTYPE.DEFAULT);
    }

    Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> userRepository.findByUsername(new Username(username)).orElseThrow();
    }
}