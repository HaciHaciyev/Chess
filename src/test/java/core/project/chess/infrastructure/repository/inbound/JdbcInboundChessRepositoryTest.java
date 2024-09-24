package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.entities.ChessGameTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Transactional
class JdbcInboundChessRepositoryTest {

    @Inject
    JdbcInboundChessRepository jdbcInboundChessRepository;

    @Test
    void completelySaveStartedChessGame() {
        //final ChessGame chessGame = ChessGameTest.defaultChessGameSupplier().get();
        //jdbcInboundChessRepository.completelySaveStartedChessGame(chessGame);
    }

    @Test
    void completelyUpdateCompletedGame() {
    }
}