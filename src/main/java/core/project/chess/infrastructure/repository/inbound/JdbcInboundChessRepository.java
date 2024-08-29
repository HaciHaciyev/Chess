package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JDBC jdbc;

    JdbcInboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveStartedChessGame(ChessGame chessGame) {

    }

    @Override
    public void updateCompletedGame(ChessGame chessGame) {

    }
}
