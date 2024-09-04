package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.user.value_objects.Rating;

import static java.lang.Math.*;

/**
 * The Glicko2RatingCalculator class provides methods to calculate the Glicko-2 ratings for players
 * based on the results of chess games. The Glicko-2 rating system is an improvement over the Elo rating
 * system, incorporating additional factors such as rating deviation and volatility to provide a more
 * accurate representation of a player's skill level.
 * <p>
 * <a href="http://www.glicko.net/glicko/glicko2.pdf">The source of the algorithm implemented in this class.</a>
 *
 * @author Hadzhyiev Hadzhy
 */
public class Glicko2RatingCalculator {

    private static final double τ = 0.5;

    private static final double ε = 0.000001;

    private Glicko2RatingCalculator() {}

    public static Rating calculate(final Rating rating, final Rating opponentRating, final double result) {
        if (result != 0 && result != 0.5 && result != 1) {
            throw new IllegalArgumentException("Result must be 0 (lose), 0.5 (draw) or 1 (win) but was " + result);
        }

        /* μ.*/ final double ratingInGlicko2Scale = (rating.rating() - 1500) / 173.7178;

        /* φ.*/ final double ratingDeviationInGlicko2Scale = rating.ratingDeviation() / 173.7178;

        /* σ.*/ final double ratingVolatilityInGlicko2Scale = rating.volatility();

        /* μ.*/ final double opponentRatingInGlicko2Scale = (opponentRating.rating() - 1500) / 173.7178;

        /* φ.*/ final double opponentRatingDeviationInGlicko2Scale = opponentRating.ratingDeviation() / 173.7178;

        // v.
        final double ratingDispersion = ratingDispersion(
                /* μ.*/  ratingInGlicko2Scale,
                /* μj.*/ opponentRatingInGlicko2Scale,
                /* φj.*/  opponentRatingDeviationInGlicko2Scale
        );

        // Δ.
        final double delta = delta(
                /* v. */ ratingDispersion,
                /* μ.*/  ratingInGlicko2Scale,
                /* μj.*/ opponentRatingInGlicko2Scale,
                /* φj.*/ opponentRatingDeviationInGlicko2Scale,
                /* sj.*/ result
        );

        // a - a = ln(o^2).
        final double ratingVariance = log(
                pow(ratingVolatilityInGlicko2Scale, 2)
        );

        // σ′.
        final double resultOfVolatility = computeNewVolatility(
                /* a.*/ ratingVariance,
                /* v.*/ ratingDispersion,
                /* Δ.*/ delta,
                /* φ.*/ ratingDeviationInGlicko2Scale
        );

        // φ′.
        final double preResultOfRatingDeviation = resultRatingDeviationInGlicko2Scale(
                /* φ*.*/updateRatingDeviation(ratingDeviationInGlicko2Scale, resultOfVolatility), ratingDispersion
        );

        // μ′.
        final double preResultRating = updateRating(
                /* μ.*/  ratingInGlicko2Scale,
                /* φ′.*/ preResultOfRatingDeviation,
                /* μj.*/ opponentRatingInGlicko2Scale,
                /* φj.*/ opponentRatingDeviationInGlicko2Scale,
                /* sj.*/ result
        );

        /* R1.*/  final double resultOfRating = 173.7178 * preResultRating + 1500;

        /* RD1.*/ final double resultOfRatingDeviation = 173.7178 * preResultOfRatingDeviation;

        return Rating.fromRepository(resultOfRating, resultOfRatingDeviation, resultOfVolatility);
    }

    /** .*/
    private static double updateRating(final double μ, final double φ1, final double μj, final double φj, final double sj) {
        return μ + pow(φ1, 2) * adjustmentFunction(φj) * (sj - expectedScoreFunction(μ, μj, φj));
    }

    /** .*/
    private static double resultRatingDeviationInGlicko2Scale(final double φ01, final double v) {
        return 1 / sqrt(
                (1 / pow(φ01, 2)) + (1 / v)
        );
    }

    /** .*/
    private static double updateRatingDeviation(final double φ, final double σ1) {
        // φ*
        return sqrt(
                pow(φ, 2) + pow(σ1, 2)
        );
    }

    /** .*/
    private static double computeNewVolatility(final double a, final double v, final double delta, final double φ) {
        double A = a;

        double B = 0;
        if (pow(delta, 2) > pow(φ, 2) + v) {
            B = log(
                    pow(delta, 2) - pow(φ, 2) - v
            );
        }

        if (pow(delta, 2) <= pow(φ, 2) + v) {
            double k = 1;
            final double fk = f(a - (k * τ), a, v, delta, φ);

            if (fk < 0) {
                k += 1;
            }

            B = a - (k * τ);
        }

        double fA = f(A, a, v, delta, φ);
        double fB = f(B, a, v, delta, φ);

        while (abs(B - A) > ε) {
            final double C = A + (A - B) * fA / (fB - fA);
            final double fC = f(C, a, v, delta, φ);

            if (fC * fB <= 0) {
                A = B;
                fA = fB;
            } else {
                fA /= 2;
            }

            B = C;
            fB = fC;
        }

        return pow(E, A / 2);
    }

    /** .*/
    private static double f(final double x, final double a, final double v, final double delta, final double φ) {
        final double firstUpperLineOfFormula = pow(E, x) * (pow(delta, 2) - pow(φ, 2) - v - pow(E, x));
        final double firstLowerLineOfFormula = 2 * pow(
                pow(φ, 2) + v + pow(E, x), 2
        );

        final double firstHalfOfTheFormula = firstUpperLineOfFormula / firstLowerLineOfFormula;
        final double secondHalfOfTheFormula = (x - a) / pow(τ, 2);

        return firstHalfOfTheFormula - secondHalfOfTheFormula;
    }

    /** .*/
    private static double delta(double v, final double μ, final double μj, final double φj, final double sj) {
        final double delta = adjustmentFunction(φj) * (sj - expectedScoreFunction(μ, μj, φj));
        return v * delta;
    }

    /** .*/
    private static double ratingDispersion(final double μ, final double μj, final double φj) {
        final double expectedScore = expectedScoreFunction(μ, μj, φj);

        return 1 / abs(
                pow(adjustmentFunction(φj), 2) * expectedScore * (1 - expectedScore)
        );
    }

    /** .*/
    private static double adjustmentFunction(double φ) {
        return 1 / sqrt(1 + (3 * pow(φ, 2)) / pow(PI, 2));
    }

    /** .*/
    private static double expectedScoreFunction(final double μ, final double μj, final double φj) {
        return 1 / (1 + exp(-adjustmentFunction(φj) * (μ - μj)));
    }
}
