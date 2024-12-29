package core.project.chess.domain.user.value_objects;

public class Rating {
    private final double rating;
    private final double ratingDeviation;
    private final double volatility;

    private Rating(double rating, double ratingDeviation, double volatility) {
        this.ratingDeviation = ratingDeviation;
        this.volatility = volatility;
        if (rating <= 0 || ratingDeviation <= 0 || volatility < 0.3 || volatility > 1.2) {
            throw new IllegalArgumentException("Invalid number.");
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
        return new Rating(Math.round(rating), Math.round(ratingDeviation), volatility);
    }

    public static Rating defaultRating() {
        return new Rating(1500.00d, 350.00d, 0.6);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rating rating1)) return false;

        return Double.compare(rating, rating1.rating) == 0 &&
                Double.compare(ratingDeviation, rating1.ratingDeviation) == 0 &&
                Double.compare(volatility, rating1.volatility) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(rating);
        result = 31 * result + Double.hashCode(ratingDeviation);
        result = 31 * result + Double.hashCode(volatility);
        return result;
    }

    @Override
    public String toString() {
        return String.format("""
                Rating : %f,
                Deviation : %f,
                Volatility : %f
                """,
                this.rating, this.ratingDeviation, this.volatility
        );
    }
}
