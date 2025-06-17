package core.project.chess.domain.user.value_objects;

import core.project.chess.domain.commons.value_objects.Rating;

import java.util.Objects;

public record Ratings(Rating rating, Rating bulletRating, Rating blitzRating, Rating rapidRating, Rating puzzlesRating) {

    public Ratings {
        Objects.requireNonNull(rating);
        Objects.requireNonNull(bulletRating);
        Objects.requireNonNull(blitzRating);
        Objects.requireNonNull(rapidRating);
        Objects.requireNonNull(puzzlesRating);
    }

    public static Ratings defaultRatings() {
        return new Ratings(Rating.defaultRating(), Rating.defaultRating(),
                Rating.defaultRating(), Rating.defaultRating(), Rating.defaultRating());
    }

    public static Ratings newRating(Ratings ratings, Rating rating) {
        return new Ratings(rating, ratings.bulletRating, ratings.blitzRating, ratings.rapidRating, ratings.puzzlesRating);
    }

    public static Ratings newBulletRating(Ratings ratings, Rating bulletRating) {
        return new Ratings(ratings.rating, bulletRating, ratings.blitzRating, ratings.rapidRating, ratings.puzzlesRating);
    }

    public static Ratings newBlitzRating(Ratings ratings, Rating blitzRating) {
        return new Ratings(ratings.rating, ratings.bulletRating, blitzRating, ratings.rapidRating, ratings.puzzlesRating);
    }

    public static Ratings newRapidRating(Ratings ratings, Rating rapidRating) {
        return new Ratings(ratings.rating, ratings.bulletRating, ratings.blitzRating, rapidRating, ratings.puzzlesRating);
    }

    public static Ratings newPuzzlesRating(Ratings ratings, Rating puzzlesRating) {
        return new Ratings(ratings.rating, ratings.bulletRating, ratings.blitzRating, ratings.rapidRating, puzzlesRating);
    }
}
