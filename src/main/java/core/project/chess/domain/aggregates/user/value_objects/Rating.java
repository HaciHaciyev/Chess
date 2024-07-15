package core.project.chess.domain.aggregates.user.value_objects;

public record Rating(short rating) {

    public Rating {
        if (rating < 1400) {
            throw new IllegalArgumentException("Rating cannot be negative or less than 1400");
        }
    }
}
