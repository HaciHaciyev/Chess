package core.project.chess.domain.aggregates.user.value_objects;

public class Rating {
    private final double rating;
    private final double ratingDeviation;
    private final double volatility;

    private Rating(double rating, double ratingDeviation, double volatility) {
        this.ratingDeviation = ratingDeviation;
        this.volatility = volatility;
        if (rating <= 0 || ratingDeviation <= 0 || volatility <= 0) {
            throw new IllegalArgumentException("Rating cannot be negative or equals 0");
        }

        this.rating = rating;
    }

    public double rating() {
        return rating;
    }

    public double ratingDeviation() {
        return ratingDeviation;
    }

    public double volatility() {
        return volatility;
    }

    public static Rating fromRepository(double rating, double ratingDeviation, double volatility) {
        return new Rating(rating, ratingDeviation, volatility);
    }

    public static Rating defaultRating() {
        return new Rating(1500.00d, 350.00d, 0.6);
    }
}
