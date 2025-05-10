package core.project.chess.infrastructure.dal.repository;

import com.hadzhy.jdbclight.jdbc.JDBC;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import static com.hadzhy.jdbclight.sql.SQLBuilder.*;

@Transactional
@ApplicationScoped
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JDBC jdbc;

    static final String SAVE_STARTED_CHESS_GAME = batchOf(
            insert()
            .into("ChessGame")
            .column("id")
            .column("player_for_white_rating")
            .column("player_for_black_rating")
            .column("time_controlling_type")
            .column("creation_date")
            .column("last_updated_date")
            .column("is_game_over")
            .column("game_result_status")
            .values()
            .build().toSQlQuery(),
            insert()
            .into("GamePlayers")
            .columns("chess_game_id", "player_for_white_id", "player_for_black_id")
            .values()
            .build().toSQlQuery());

    static final String SAVE_CHESS_GAME_HISTORY = insert()
            .into("ChessGameHistory")
            .columns("id", "chess_game_id", "pgn_chess_representation")
            .values()
            .build()
            .sql();

    static final String UPDATE_FINISHED_CHESS_GAME = update("ChessGame")
            .set("is_game_over = ?, game_result_status = ?")
            .where("id = ?")
            .build()
            .sql();

    static final String SAVE_PUZZLE = insert()
            .into("Puzzle")
            .column("id")
            .column("rating")
            .column("rating_deviation")
            .column("rating_volatility")
            .column("startPositionFEN")
            .column("pgn")
            .column("startPositionIndex")
            .values()
            .build()
            .sql();

    static final String SAVE_PUZZLE_SOLVING = batchOf(
            update("Puzzle")
            .set("rating = ?, rating_deviation = ?, rating_volatility = ?")
            .where("id = ?")
            .build().toSQlQuery(),
            insert()
            .into("UserPuzzles")
            .columns("puzzle_id", "user_id", "is_solved")
            .values()
            .build().toSQlQuery());

    JdbcInboundChessRepository() {
        this.jdbc = JDBC.instance();
    }

    @Override
    public void completelySaveStartedChessGame(final ChessGame chessGame) {
        if (chessGame.isGameOver())
            throw new IllegalArgumentException("You can`t save finished game as new one");

        jdbc.write(SAVE_STARTED_CHESS_GAME,
                        chessGame.chessGameID().toString(),
                        chessGame.whiteRating().rating(),
                        chessGame.blackRating().rating(),
                        chessGame.time().toString(),
                        chessGame.sessionEvents().creationDate(),
                        chessGame.sessionEvents().lastUpdateDate(),
                        chessGame.isGameOver(),
                        chessGame.gameResult().toString(),
                        chessGame.chessGameID().toString(),
                        chessGame.whitePlayer().id().toString(),
                        chessGame.blackPlayer().id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void completelyUpdateFinishedGame(final ChessGame chessGame) {
        if (!chessGame.isGameOver()) throw new IllegalArgumentException("Game is not over.");

        jdbc.write(UPDATE_FINISHED_CHESS_GAME,
                        chessGame.isGameOver(),
                        chessGame.gameResult().toString(),
                        chessGame.chessGameID().toString())
                .ifFailure(Throwable::printStackTrace);

        jdbc.write(SAVE_CHESS_GAME_HISTORY,
                        chessGame.historyID().toString(),
                        chessGame.chessGameID().toString(),
                        chessGame.pgn())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void savePuzzle(Puzzle puzzle) {
        jdbc.asynchWrite(SAVE_PUZZLE,
                        puzzle.ID().toString(),
                        puzzle.rating().rating(),
                        puzzle.rating().ratingDeviation(),
                        puzzle.rating().volatility(),
                        puzzle.startPositionFEN(),
                        puzzle.PGN(),
                        puzzle.startPositionIndex())
                .exceptionally(throwable -> {
                    Log.error("Error saving puzzle.", throwable);
                    return null;
                });
    }

    @Override
    public void updatePuzzleOnSolving(final Puzzle puzzle) {
        if (!puzzle.isEnded()) throw new IllegalArgumentException("Puzzle is not ended.");

        jdbc.write(SAVE_PUZZLE_SOLVING,
                        puzzle.rating().rating(),
                        puzzle.rating().ratingDeviation(),
                        puzzle.rating().volatility(),
                        puzzle.ID().toString(),
                        puzzle.player().id().toString(),
                        puzzle.isSolved())
                .ifFailure(Throwable::printStackTrace);
    }
}