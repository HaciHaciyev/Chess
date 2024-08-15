package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcInboundChessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveStarterChessGame(final ChessGame chessGame) {

    }

    @Override
    public void updateCompletedGame(final ChessGame chessGame) {

    }
}
