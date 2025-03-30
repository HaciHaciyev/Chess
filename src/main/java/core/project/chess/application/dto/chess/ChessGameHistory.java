package core.project.chess.application.dto.chess;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.value_objects.Username;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChessGameHistory(
        UUID chessHistoryId,
        String pgn,
        Username playerForWhite,
        Username playerForBlack,
        ChessGame.Time timeControl,
        GameResult gameResult,
        double whitePlayerRating,
        double blackPlayerRating,
        LocalDateTime gameStart,
        LocalDateTime gameEnd) {

    @Override
    public String toString() {
        return String.format("""
                ChessGameHistory {
                    Id : %s,
                    Username of white player: %s,
                    Username of black player: %s,
                    Time: %s,
                    Game Result: %s,
                    Rating of white player: %f,
                    Rating of black player: %f,
                    PGN : %s,
                    Game start: %s,
                    Game end: %s.
                }
                """,
                this.chessHistoryId.toString(),
                this.playerForWhite.username(),
                this.playerForBlack.username(),
                this.timeControl.toString(),
                this.gameResult.toString(),
                this.whitePlayerRating,
                this.blackPlayerRating,
                this.pgn,
                this.gameStart.toString(),
                this.gameEnd.toString()
        );
    }
}
