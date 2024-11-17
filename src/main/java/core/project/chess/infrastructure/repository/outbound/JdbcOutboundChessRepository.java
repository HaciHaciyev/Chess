package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.value_objects.ChessGameHistory;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.exceptions.persistant.DataNotFoundException;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
@ApplicationScoped
public class JdbcOutboundChessRepository implements OutboundChessRepository {

    private final JDBC jdbc;

    public static final String IS_PRESENT = "SELECT COUNT(id) FROM ChessGameHistory WHERE id = ?";

    public static final String GET_PARTNERS_USERNAMES = """
            SELECT partner.username
            FROM UserPartnership AS up
            JOIN UserAccount AS partner ON up.partner_id = partner.id
            JOIN UserAccount AS user_account ON up.user_id = user_account.id
            WHERE user_account.username = ?
            
            UNION
            
            SELECT user_account.username
            FROM UserPartnership AS up
            JOIN UserAccount AS user_account ON up.user_id = user_account.id
            JOIN UserAccount AS partner ON up.partner_id = partner.id
            WHERE partner.username = ?
            LIMIT 10 OFFSET ? * 10;
            """;

    public static final String GET_CHESS_GAME = """
            SELECT
                cgh.id AS chessHistoryId,
                cgh.pgn_chess_representation AS pgn,
                cgh.fen_representations_of_board AS fenRepresentations,
            
                wa.username AS playerForWhite,
                ba.username AS playerForBlack,
            
                cg.time_controlling_type AS timeControl,
                cg.game_result_status AS gameResult,
            	cg.player_for_white_rating AS whitePlayerRating,
                cg.player_for_black_rating AS blackPlayerRating,
                cg.creation_date AS gameStart,
                cg.last_updated_date AS gameEnd
            
            FROM ChessGameHistory cgh
            JOIN ChessGame cg ON cgh.chess_game_id = cg.id
            JOIN GamePlayers gp ON cg.id = gp.chess_game_id
            JOIN UserAccount wa ON gp.player_for_white_id = wa.id
            JOIN UserAccount ba ON gp.player_for_black_id = ba.id;
            WHERE cgh.id = ?
            """;

    public static final String LIST_OF_GAMES = """
            WITH filtered_games AS (
                SELECT
                    cgh.id AS chessHistoryId,
                    cgh.pgn_chess_representation AS pgn,
                    cgh.fen_representations_of_board AS fenRepresentations,
            
                    wa.username AS playerForWhite,
                    ba.username AS playerForBlack,
            
                    cg.time_controlling_type AS timeControl,
                    cg.game_result_status AS gameResult,
            		cg.player_for_white_rating AS whitePlayerRating,
                    cg.player_for_black_rating AS blackPlayerRating,
                    cg.creation_date AS gameStart,
                    cg.last_updated_date AS gameEnd
            
                FROM ChessGameHistory cgh
                JOIN ChessGame cg ON cgh.chess_game_id = cg.id
                JOIN GamePlayers gp ON cg.id = gp.chess_game_id
                JOIN UserAccount wa ON gp.player_for_white_id = wa.id
                JOIN UserAccount ba ON gp.player_for_black_id = ba.id
                WHERE wa.username = ? OR ba.username = ?
                ORDER BY cg.creation_date DESC
            )
            SELECT * FROM filtered_games
            LIMIT 10 OFFSET ? * 10;
            """;

    JdbcOutboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isChessHistoryPresent(final UUID chessHistoryId) {
        Result<Integer, Throwable> result = jdbc.readObjectOf(
                IS_PRESENT,
                Integer.class,
                chessHistoryId.toString()
        );

        if (!result.success()) {

            if (result.throwable() instanceof DataNotFoundException) {
                return false;
            } else {
                Log.error(result.throwable());
            }

        }

        return result.value() != null && result.value() > 0;
    }

    @Override
    public Result<ChessGameHistory, Throwable> findById(final UUID chessGameId) {
        return jdbc.read(GET_CHESS_GAME, this::chessGameMapper, Objects.requireNonNull(chessGameId));
    }

    @Override
    public Result<List<ChessGameHistory>, Throwable> listOfGames(final Username username, final int pageNumber) {
        return jdbc.readListOf(
                LIST_OF_GAMES,
                this::chessGameMapper,
                Objects.requireNonNull(username).username(),
                Objects.requireNonNull(username).username(),
                pageNumber
        );
    }

    @Override
    public Result<List<String>, Throwable> listOfPartners(String username, int pageNumber) {
        return jdbc.readListOf(
                GET_PARTNERS_USERNAMES,
                rs -> rs.getString("username"),
                Objects.requireNonNull(username),
                Objects.requireNonNull(username),
                pageNumber
        );
    }

    private ChessGameHistory chessGameMapper(final ResultSet rs) throws SQLException {

        return new ChessGameHistory(
                UUID.fromString(rs.getString("chessHistoryId")),
                rs.getString("pgn"),
                (String[]) rs.getArray("fenRepresentations").getArray(),
                new Username(rs.getString("playerForWhite")),
                new Username(rs.getString("playerForBlack")),
                ChessGame.TimeControllingTYPE.valueOf(rs.getString("timeControl")),
                GameResult.valueOf(rs.getString("gameResult")),
                rs.getDouble("whitePlayerRating"),
                rs.getDouble("blackPlayerRating"),
                rs.getObject("gameStart", Timestamp.class).toLocalDateTime(),
                rs.getObject("gameEnd", Timestamp.class).toLocalDateTime()
        );
    }
}
