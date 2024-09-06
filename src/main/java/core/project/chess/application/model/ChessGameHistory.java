package core.project.chess.application.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record ChessGameHistory(UUID chessHistoryId, String pgn, String[] fenRepresentations) {

    public ChessGameHistory {
        Objects.requireNonNull(chessHistoryId);
        Objects.requireNonNull(pgn);
        Objects.requireNonNull(fenRepresentations);
        if (pgn.isBlank()) {
            throw new IllegalArgumentException("PGN can`t be blank.");
        }
        for (final String fenRepresentation : fenRepresentations) {
            Objects.requireNonNull(fenRepresentation);

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
