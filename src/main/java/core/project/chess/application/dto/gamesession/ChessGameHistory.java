package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.user.value_objects.Username;

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
        ChessGame.TimeControllingTYPE timeControl,
        GameResult gameResult,
        double whitePlayerRating,
        double blackPlayerRating,
        LocalDateTime gameStart,
        LocalDateTime gameEnd) {

    public ChessGameHistory {
        Objects.requireNonNull(chessHistoryId);
        Objects.requireNonNull(pgn);
        Objects.requireNonNull(fenRepresentations);
        Objects.requireNonNull(playerForWhite);
        Objects.requireNonNull(playerForBlack);
        Objects.requireNonNull(gameStart);
        Objects.requireNonNull(gameEnd);

        if (pgn.isBlank()) {
            throw new IllegalArgumentException("PGN can`t be blank.");
        }
        for (final String fenRepresentation : fenRepresentations) {
            if (fenRepresentation.isBlank()) {
                throw new IllegalArgumentException("FEN representation can`t be blank.");
            }
        }

    }

    public String fenRepresentation(final int index) {
        return fenRepresentations[index];
    }

    @Override
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
                PGN : %s,
                FEN : %s.
                }
                """,
                this.chessHistoryId.toString(), this.pgn, Arrays.toString(this.fenRepresentations)
        );
    }
}
