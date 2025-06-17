package core.project.chess.domain.commons.value_objects;

import java.util.Objects;
import java.util.UUID;

public record RatingUpdateOnPuzzle(UUID gameID, UUID playerID, Rating puzzleRating, Rating playerRating, PuzzleStatus gameResult) {
    public RatingUpdateOnPuzzle {
        Objects.requireNonNull(gameID);
        Objects.requireNonNull(playerID);
        Objects.requireNonNull(gameResult);
        Objects.requireNonNull(puzzleRating);
        Objects.requireNonNull(playerRating);
        if (gameID.equals(playerID))
            throw new IllegalArgumentException("Do not match");
    }
}
