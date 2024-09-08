package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.application.model.ChessGameHistory;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

@Transactional
@ApplicationScoped
public class JdbcOutboundChessRepository implements OutboundChessRepository {

    private final JDBC jdbc;

    JdbcOutboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Result<ChessGameHistory, Throwable> findById(final UUID chessGameId) {
        Objects.requireNonNull(chessGameId);

        final String sql = """
                SELECT id, pgn_chess_representation, fen_representations_of_board
                WHERE chess_game_id = ?
                """;

        return jdbc.query(sql, this::chessGameMapper, chessGameId);
    }

    private ChessGameHistory chessGameMapper(final ResultSet rs) throws SQLException {
        Log.info("ChessGame is recreated from repository.");

        final Array array = rs.getArray("fen_representations_of_board");
        return new ChessGameHistory(
                UUID.fromString(rs.getString("id")),
                rs.getString("pgn_chess_representation"),
                (String[]) array.getArray()
        );
    }
}
