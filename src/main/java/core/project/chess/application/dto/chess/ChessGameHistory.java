package core.project.chess.application.dto.chess;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.value_objects.Username;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record ChessGameHistory(
        UUID chessHistoryId,
        String pgn,
        String[] fenRepresentations,
        Username playerForWhite,
        Username playerForBlack,
        ChessGame.Time timeControl,
        GameResult gameResult,
        double whitePlayerRating,
        double blackPlayerRating,
        LocalDateTime gameStart,
        LocalDateTime gameEnd) {

    public String fenRepresentation(final int index) {
        return fenRepresentations[index];
    }

    public String[] fenRepresentations() {
        final String[] result = new String[fenRepresentations.length];
        System.arraycopy(fenRepresentations, 0, result, 0, fenRepresentations.length);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessGameHistory that)) return false;

        return Objects.equals(pgn, that.pgn) && Arrays.equals(fenRepresentations, that.fenRepresentations);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(pgn);
        result = 31 * result + Arrays.hashCode(fenRepresentations);
        return result;
    }

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
                    FEN : %s,
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
                Arrays.toString(this.fenRepresentations),
                this.gameStart.toString(),
                this.gameEnd.toString()
        );
    }
}
