package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.user.value_objects.Rating;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Glicko2RatingCalculatorTest {

    @Test
    void calculate() {
        final Rating firstRating = Rating.fromRepository(1500, 200, 0.6);
        final Rating secondRating = Rating.fromRepository(1400, 30, 0.6);

        final Rating firstResult = Glicko2RatingCalculator.calculate(firstRating, secondRating, 1);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondRating, firstRating, 0);

        if (firstResult.volatility() != firstRating.volatility()) {
            final String message = String.format(
                    "Volatility of first player changed. Previous volatility : %f. Current volatility : %f.",
                    firstRating.volatility(),
                    firstResult.volatility()
            );

            Log.info(message);
        }

        if (secondResult.volatility() != secondRating.volatility()) {
            final String message = String.format(
                    "Volatility of second player changed. Previous volatility : %f. Current volatility : %f.",
                    secondRating.volatility(),
                    secondResult.volatility()
            );

            Log.info(message);
        }

        assertEquals(1576, firstResult.rating());
        assertEquals(192, firstResult.ratingDeviation());
        Log.info("Test for player that win ended successfully.");

        assertEquals(1380, secondResult.rating());
        assertEquals(105, secondResult.ratingDeviation());
        Log.info("Test for player that lose ended successfully.");
    }

    @Test
    void calculateWithDrawResult() {
        final Rating firstRating = Rating.fromRepository(1483, 144, 0.6);
        final Rating secondRating = Rating.fromRepository(1284, 234, 0.6);

        final Rating firstResult = Glicko2RatingCalculator.calculate(firstRating, secondRating, 0.5);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondRating, firstRating, 0.5);

        if (firstResult.volatility() != firstRating.volatility()) {
            final String message = String.format(
                    "Volatility of first player changed. Previous volatility : %f. Current volatility : %f.",
                    firstRating.volatility(),
                    firstResult.volatility()
            );

            Log.info(message);
        }

        if (secondResult.volatility() != secondRating.volatility()) {
            final String message = String.format(
                    "Volatility of second player changed. Previous volatility : %f. Current volatility : %f.",
                    secondRating.volatility(),
                    secondResult.volatility()
            );

            Log.info(message);
        }

        assertEquals(1455, firstResult.rating());
        assertEquals(167, firstResult.ratingDeviation());
        Log.info("Test for player that end game with draw ended successfully.");

        assertEquals(1345, secondResult.rating());
        assertEquals(221, secondResult.ratingDeviation());
        Log.info("Test for player that end game with draw ended successfully.");
    }
}