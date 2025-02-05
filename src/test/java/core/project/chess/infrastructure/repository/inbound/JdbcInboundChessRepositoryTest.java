package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.dal.repository.inbound.JdbcInboundChessRepository;
import core.project.chess.infrastructure.dal.repository.outbound.JdbcOutboundUserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
@Transactional
@Disabled("It is no longer relevant, since repositories are already tested via common endpoints, and is also disabled because it does not use test containers and pollutes the main database.")
class JdbcInboundChessRepositoryTest {

    @Inject
    JdbcOutboundUserRepository userRepository;

    @Inject
    JdbcInboundChessRepository jdbcInboundChessRepository;

    @Test
    void resignation() {
        assertDoesNotThrow  (() -> {
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

    @Test
    void agreement() {
        assertDoesNotThrow(() -> {
            final ChessGame chessGame = defaultChessGameSupplier("HHadzhy", "AinGrace").get();
            jdbcInboundChessRepository.completelySaveStartedChessGame(chessGame);

            final String firstUsername = chessGame.getPlayerForWhite().getUsername().username();
            final String secondUsername = chessGame.getPlayerForBlack().getUsername().username();

            chessGame.makeMovement(firstUsername, Coordinate.e2, Coordinate.e4, null);
            chessGame.makeMovement(secondUsername, Coordinate.e7, Coordinate.e5, null);

            chessGame.makeMovement(firstUsername, Coordinate.g1, Coordinate.f3, null);

            chessGame.agreement(firstUsername);
            chessGame.agreement(secondUsername);

            jdbcInboundChessRepository.completelyUpdateFinishedGame(chessGame);
        });
    }

    Supplier<ChessGame> defaultChessGameSupplier(final String first, final String second) {
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard();

        return () -> ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                userAccountSupplier(first).get(),
                userAccountSupplier(second).get(),
                SessionEvents.defaultEvents(),
                ChessGame.Time.DEFAULT, false);
    }

    Supplier<UserAccount> userAccountSupplier(String username) {
        return () -> userRepository.findByUsername(new Username(username)).orElseThrow();
    }
}