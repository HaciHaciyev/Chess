package core.project.chess.infrastructure.dal.repository.outbound;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.dal.util.jdbc.JDBC;
import core.project.chess.infrastructure.dal.util.sql.Order;
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

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.*;

@Transactional
@ApplicationScoped
public class JdbcOutboundChessRepository implements OutboundChessRepository {

    private final JDBC jdbc;

    static final String IS_PRESENT = select()
            .count("id")
            .from("ChessGameHistory")
            .where("id = ?")
            .build();

    static final String GET_CHESS_GAME = select()
            .column("cgh.id").as("chessHistoryId")
            .column("cgh.pgn_chess_representation").as("pgn")
            .column("cgh.fen_representations_of_board").as("fenRepresentations")
            .column("wa.username").as("playerForWhite")
            .column("ba.username").as("playerForBlack")
            .column("cg.time_controlling_type").as("timeControl")
            .column("cg.game_Result_status").as("gameResult")
            .column("cg.player_for_white_rating").as("whitePlayerRating")
            .column("cg.player_for_black_rating").as("blackPlayerRating")
            .column("cg.creation_date").as("gameStart")
            .column("cg.last_updated_date").as("gameEnd")
            .from("ChessGameHistory cgh")
            .join("ChessGame cg", "cgh.chess_game_id = cg.id")
            .join("GamePlayers gp", "cg.id = gp.chess_game_id")
            .join("UserAccount wa", "gp.player_for_white_id = wa.id")
            .join("UserAccount ba", "gp.player_for_black_id = ba.id")
            .where("cgh.id = ?")
            .build();

    static final String LIST_OF_GAMES = withAndSelect(
            "filtered_games", select()
                    .column("cgh.id").as("chessHistoryId")
                    .column("cgh.pgn_chess_representation").as("pgn")
                    .column("cgh.fen_representations_of_board").as("fenRepresentations")
                    .column("wa.username").as("playerForWhite")
                    .column("ba.username").as("playerForBlack")
                    .column("cg.time_controlling_type").as("timeControl")
                    .column("cg.game_Result_status").as("gameResult")
                    .column("cg.player_for_white_rating").as("whitePlayerRating")
                    .column("cg.player_for_black_rating").as("blackPlayerRating")
                    .column("cg.creation_date").as("gameStart")
                    .column("cg.last_updated_date").as("gameEnd")
                    .from("ChessGameHistory cgh")
                    .join("ChessGame cg", "cgh.chess_game_id = cg.id")
                    .join("GamePlayers gp", "cg.id = gp.chess_game_id")
                    .join("UserAccount wa", "gp.player_for_white_id = wa.id")
                    .join("UserAccount ba", "gp.player_for_black_id = ba.id")
                    .where("wa.username = ?")
                    .or("ba.username = ?")
                    .orderBy("cg.creation_date", Order.DESC)
                    .build())
            .all()
            .from("filtered_games")
            .limitAndOffset();

    JdbcOutboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isChessHistoryPresent(final UUID chessHistoryId) {
        return jdbc.readObjectOf(IS_PRESENT, Integer.class, chessHistoryId.toString())
                .mapSuccess(count -> count != null && count > 0)
                .orElseGet(() -> {
                    Log.error("Unexpected error in repository. Can`t get chess history data.");
                    return false;
                });
    }

    @Override
    public Result<ChessGameHistory, Throwable> findById(final UUID chessGameId) {
        return jdbc.read(GET_CHESS_GAME, this::chessGameMapper, Objects.requireNonNull(chessGameId.toString()));
    }

    @Override
    public Result<List<ChessGameHistory>, Throwable> listOfGames(final Username username, final int limit, final int offSet) {
        return jdbc.readListOf(LIST_OF_GAMES, this::chessGameMapper, Objects.requireNonNull(username).username(), username.username(), limit, offSet);
    }

    private ChessGameHistory chessGameMapper(final ResultSet rs) throws SQLException {

        return new ChessGameHistory(
                UUID.fromString(rs.getString("chessHistoryId")),
                rs.getString("pgn"),
                (String[]) rs.getArray("fenRepresentations").getArray(),
                new Username(rs.getString("playerForWhite")),
                new Username(rs.getString("playerForBlack")),
                ChessGame.Time.valueOf(rs.getString("timeControl")),
                GameResult.valueOf(rs.getString("gameResult")),
                rs.getDouble("whitePlayerRating"),
                rs.getDouble("blackPlayerRating"),
                rs.getObject("gameStart", Timestamp.class).toLocalDateTime(),
                rs.getObject("gameEnd", Timestamp.class).toLocalDateTime()
        );
    }
}
