package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Objects;

@Transactional
@ApplicationScoped
public class JdbcInboundChessRepository implements InboundChessRepository {

    private final JDBC jdbc;

    JdbcInboundChessRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void completelySaveStartedChessGame(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);

        final String sql = """
                    INSERT INTO ChessGame
                        (id, chess_board_id, player_for_white_rating,
                        player_for_black_rating, time_controlling_type,
                        creation_date, last_updated_date, is_game_over)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    
                    INSERT INTO ChessGamePlayers
                        (chess_game_id, player_for_white_id, player_for_black_id)
                        VALUES (?, ?, ?);
                    """;

        jdbc.update(sql,

            chessGame.getChessGameId().toString(),
            chessGame.getChessBoard().getChessBoardId().toString(),
            chessGame.getPlayerForBlackRating().rating(),
            chessGame.getPlayerForBlackRating().rating(),
            chessGame.getTimeControllingTYPE(),
            chessGame.getSessionEvents().creationDate(),
            chessGame.getSessionEvents().lastUpdateDate(),
            false,

            chessGame.getChessGameId().toString(),
            chessGame.getPlayerForWhite().getId().toString(),
            chessGame.getPlayerForBlack().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void completelyUpdateCompletedGame(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("Game is not over.");
        }

        final String sql = """
                    UPDATE ChessGame
                        is_game_over = ?
                        Where id = ?;
                    
                    INSERT INTO ChessGameHistory
                        id = ?,
                        chess_game_id = ?,
                        pgn_chess_representation = ?,
                        fen_representations_of_board = ?
                    """;

        final String arrayDefinition = "fen_representations_of_board";
        final byte arrayIndex = 6;

        jdbc.updateAndArrayStoring(sql, arrayDefinition, arrayIndex, chessGame.getChessBoard().arrayOfFEN(),

            chessGame.gameResult().isPresent(),
            chessGame.getChessGameId().toString(),
            chessGame.getChessBoard().getChessBoardId().toString(),
            chessGame.getChessGameId().toString(),
            chessGame.getChessBoard().pgn()
        )

        .ifFailure(Throwable::printStackTrace);
    }
}
