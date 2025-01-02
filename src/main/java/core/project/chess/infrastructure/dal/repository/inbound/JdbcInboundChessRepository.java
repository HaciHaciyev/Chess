package core.project.chess.infrastructure.dal.repository.inbound;

import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.infrastructure.dal.util.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Transactional
@ApplicationScoped
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JDBC jdbc;

    public static final String SAVE_STARTED_CHESS_GAME = """
            INSERT INTO ChessGame
                (id, player_for_white_rating, player_for_black_rating,
                 time_controlling_type, creation_date, last_updated_date,
                 is_game_over, game_result_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            
            INSERT INTO GamePlayers
                (chess_game_id, player_for_white_id, player_for_black_id)
                VALUES (?, ?, ?);
            """;

    public static final String UPDATE_FINISHED_CHESS_GAME = """
            UPDATE ChessGame SET
                is_game_over = ?,
                game_result_status = ?
                Where id = ?;
            """;

    public static final String SAVE_CHESS_GAME_HISTORY = """
            INSERT INTO ChessGameHistory
                (id, chess_game_id, pgn_chess_representation, fen_representations_of_board)
                VALUES (?, ?, ?, ?);
            """;

    JdbcInboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void completelySaveStartedChessGame(final ChessGame chessGame) {
        jdbc.write(SAVE_STARTED_CHESS_GAME,

            chessGame.getChessGameId().toString(),
            chessGame.getPlayerForBlackRating().rating(),
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
            chessGame.getChessBoard().getChessBoardId().toString(),
            chessGame.getChessGameId().toString(),
            chessGame.getChessBoard().pgn()
        )

        .ifFailure(Throwable::printStackTrace);
    }
}
