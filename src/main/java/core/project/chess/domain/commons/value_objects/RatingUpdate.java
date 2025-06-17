package core.project.chess.domain.commons.value_objects;

import java.util.Objects;
import java.util.UUID;

public record RatingUpdate(UUID gameID, UUID whitePlayerID, Rating whitePlayerRating,
                           UUID blackPlayerID, Rating blackPlayerRating,
                           GameResult gameResult, RatingType ratingType) {
    public RatingUpdate {
        Objects.requireNonNull(gameID);
        Objects.requireNonNull(whitePlayerID);
        Objects.requireNonNull(whitePlayerRating);
        Objects.requireNonNull(blackPlayerID);
        Objects.requireNonNull(blackPlayerRating);
        Objects.requireNonNull(gameResult);
        Objects.requireNonNull(ratingType);
    }
}
