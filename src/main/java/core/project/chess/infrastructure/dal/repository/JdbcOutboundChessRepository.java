package core.project.chess.infrastructure.dal.repository;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import com.hadzhy.jetquerious.sql.Order;
import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.value_objects.Rating;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.hadzhy.jetquerious.sql.QueryForge.select;
import static com.hadzhy.jetquerious.sql.QueryForge.withAndSelect;

@ApplicationScoped
public class JdbcOutboundChessRepository implements OutboundChessRepository {

    private final JetQuerious jet;

    static final String IS_PRESENT = select()
            .count("id")
            .from("ChessGameHistory")
            .where("id = ?")
            .build()
            .sql();

    static final String GET_PUZZLE = select()
            .column("id")
            .column("rating")
            .column("rating_deviation")
            .column("rating_volatility")
            .column("pgn")
            .column("startPositionIndex")
            .from("Puzzle")
            .where("id = ?")
            .build()
            .sql();

    static final String RANDOM_PUZZLE = select()
            .column("id")
            .column("rating")
            .column("rating_deviation")
            .column("rating_volatility")
            .column("pgn")
            .column("startPositionIndex")
            .from("Puzzle")
            .where("rating BETWEEN ?")
            .and("?")
            .and("id NOT IN (SELECT id from UserPuzzles)")
            .limitAndOffset(1, 0)
            .sql();

    static final String LIST_OF_PUZZLES = select()
            .column("id")
            .column("rating")
            .column("rating_deviation")
            .column("rating_volatility")
            .column("pgn")
            .column("startPositionIndex")
            .from("Puzzle")
            .where("rating BETWEEN ?")
            .and("?")
            .limitAndOffset()
            .sql();

    static final String GET_CHESS_GAME = select()
            .column("cgh.id").as("chessHistoryId")
            .column("cgh.pgn_chess_representation").as("pgn")
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
            .build()
            .sql();

    static final String LIST_OF_GAMES = withAndSelect(
            "filtered_games", select()
                    .column("cgh.id").as("chessHistoryId")
                    .column("cgh.pgn_chess_representation").as("pgn")
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
                    .build().toSQlQuery())
            .all()
            .from("filtered_games")
            .limitAndOffset()
            .sql();

    JdbcOutboundChessRepository() {
        this.jet = JetQuerious.instance();
    }

    @Override
    public boolean isChessHistoryPresent(final UUID chessHistoryId) {
        return jet.readObjectOf(IS_PRESENT, Integer.class, chessHistoryId.toString())
                .mapSuccess(count -> count != null && count > 0)
                .orElseGet(() -> {
                    Log.error("Unexpected error in repository. Can`t get chess history data.");
                    return false;
                });
    }

    @Override
    public Result<ChessGameHistory, Throwable> findById(final UUID chessGameId) {
        var chessGame = jet.read(GET_CHESS_GAME, this::chessGameMapper, Objects.requireNonNull(chessGameId.toString()));
        return new Result<>(chessGame.value(), chessGame.throwable(), chessGame.success());
    }

    @Override
    public Result<List<ChessGameHistory>, Throwable> listOfGames(final Username username, final int limit, final int offSet) {
        var gamesList = jet.readListOf(LIST_OF_GAMES,
                this::chessGameMapper,
                Objects.requireNonNull(username.username()),
                username.username(),
                limit,
                offSet);

        return new Result<>(gamesList.value(), gamesList.throwable(), gamesList.success());
    }

    @Override
    public Result<Puzzle, Throwable> puzzle(double minRating, double maxRating) {
        var puzzleResult = jet.read(RANDOM_PUZZLE, this::puzzleMapper, minRating, maxRating);
        return new Result<>(puzzleResult.value(), puzzleResult.throwable(), puzzleResult.success());
    }

    @Override
    public Result<Puzzle, Throwable> puzzle(UUID puzzleId) {
        var puzzleResult = jet.read(GET_PUZZLE, this::puzzleMapper, puzzleId.toString());
        return new Result<>(puzzleResult.value(), puzzleResult.throwable(), puzzleResult.success());
    }

    @Override
    public Result<List<Puzzle>, Throwable> listOfPuzzles(double minRating, double maxRating, int limit, int offSet) {
        var puzzlesList = jet.readListOf(LIST_OF_PUZZLES, this::puzzleMapper, minRating, maxRating, limit, offSet);
        return new Result<>(puzzlesList.value(), puzzlesList.throwable(), puzzlesList.success());
    }

    ChessGameHistory chessGameMapper(final ResultSet rs) throws SQLException {
        return new ChessGameHistory(
                UUID.fromString(rs.getString("chessHistoryId")),
                rs.getString("pgn"),
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

    Puzzle puzzleMapper(ResultSet rs) throws SQLException {
        Rating rating = Rating.fromRepository(rs.getDouble("rating"),
                rs.getDouble("rating_deviation"),
                rs.getDouble("rating_volatility"));

        return new Puzzle(
                UUID.fromString(rs.getString("id")),
                rs.getString("pgn"),
                rs.getInt("startPositionIndex"),
                rating
        );
    }
}
