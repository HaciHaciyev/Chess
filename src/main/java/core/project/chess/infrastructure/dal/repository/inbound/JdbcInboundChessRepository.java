package core.project.chess.infrastructure.dal.repository.inbound;

import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.infrastructure.dal.util.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.insert;
import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.update;

@Transactional
@ApplicationScoped
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JDBC jdbc;

    static final String SAVE_STARTED_CHESS_GAME = String.format("%s; %s;",
            insert()
            .into("ChessGame")
            .columns("id",
                    "player_for_white_rating",
                    "player_for_black_rating",
                    "time_controlling_type",
                    "creation_date",
                    "last_updated_date",
                    "is_game_over",
                    "game_result_status"
            )
            .values(8)
            .build(),
            insert()
            .into("GamePlayers")
            .columns("chess_game_id",
                    "player_for_white_id",
                    "player_for_black_id"
            )
            .values(3)
            .build()
    );

    static final String SAVE_CHESS_GAME_HISTORY = insert()
            .into("ChessGameHistory")
            .columns("id",
                    "chess_game_id",
                    "pgn_chess_representation",
                    "fen_representations_of_board"
            )
            .values(4)
            .build();

    static final String UPDATE_FINISHED_CHESS_GAME = update("ChessGame")
            .set("is_game_over = ?, game_result_status = ?")
            .where("id = ?")
            .build();

    static final String SAVE_PUZZLE = String.format("%s; %s;",
            insert()
            .into("Puzzle")
            .columns("id",
                    "rating",
                    "rating_deviation",
                    "rating_volatility",
                    "startPositionFEN",
                    "pgn"
            )
            .values(6)
            .onConflict("id")
            .doUpdateSet("rating = ?, rating_deviation = ?, rating_volatility = ?")
            .build(),
            insert()
            .into("UserPuzzles")
            .columns("puzzle_id", "user_id", "is_solved")
            .values(3)
            .build()
    );

    JdbcInboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void completelySaveStartedChessGame(final ChessGame chessGame) {
        jdbc.write(SAVE_STARTED_CHESS_GAME,

            chessGame.getChessGameId().toString(),
            chessGame.getPlayerForWhiteRating().rating(),
            chessGame.getPlayerForBlackRating().rating(),
            chessGame.getTime().toString(),
            chessGame.getSessionEvents().creationDate(),
            chessGame.getSessionEvents().lastUpdateDate(),
            false, "NONE",

            chessGame.getChessGameId().toString(),
            chessGame.getPlayerForWhite().getId().toString(),
            chessGame.getPlayerForBlack().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void completelyUpdateFinishedGame(final ChessGame chessGame) {
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("Game is not over.");
        }

        jdbc.write(UPDATE_FINISHED_CHESS_GAME,
                chessGame.gameResult().isPresent(),
                chessGame.gameResult().orElseThrow().toString(),
                chessGame.getChessGameId().toString()
        )

        .ifFailure(Throwable::printStackTrace);

        final byte arrayIndex = 4;
        final String arrayDefinition = "TEXT";
        jdbc.writeArrayOf(SAVE_CHESS_GAME_HISTORY,
            arrayDefinition, arrayIndex, chessGame.getChessBoard().arrayOfFEN(),
            chessGame.getChessBoard().ID().toString(),
            chessGame.getChessGameId().toString(),
            chessGame.getChessBoard().pgn()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void savePuzzle(final Puzzle puzzle) {
        if (!puzzle.isEnded()) {
            throw new IllegalArgumentException("Puzzle is not ended.");
        }

        String puzzleID = puzzle.ID().toString();
        jdbc.write(SAVE_PUZZLE, puzzleID,
                        puzzle.rating().rating(),
                        puzzle.rating().ratingDeviation(),
                        puzzle.rating().volatility(),
                        puzzle.startPositionFEN(),
                        puzzle.PGN(),
                        puzzle.rating().ratingDeviation(),
                        puzzle.rating().volatility(),
                        puzzleID,
                        puzzle.player().getId().toString(),
                        puzzle.isSolved())
                .ifFailure(Throwable::printStackTrace);
    }
}
