package core.project.chess.domain.user.util;

import core.project.chess.domain.user.value_objects.Rating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Glicko2RatingCalculatorTest {

    @Test
    void calculate() {
        final Rating firstRating = Rating.fromRepository(1500, 200, 0.6);
        final Rating secondRating = Rating.fromRepository(1400, 30, 0.6);

        final Rating firstResult = Glicko2RatingCalculator.calculate(firstRating, secondRating, 1);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondRating, firstRating, 0);

        assertEquals(1576, firstResult.rating());
        assertEquals(192, firstResult.ratingDeviation());

        assertEquals(1380, secondResult.rating());
        assertEquals(105, secondResult.ratingDeviation());
    }

    @Test
    void calculate2() {
        final Rating firstRating = Rating.fromRepository(1500, 350, 0.6);
        final Rating secondRating = Rating.fromRepository(1500, 350, 0.6);

        final Rating firstResult = Glicko2RatingCalculator.calculate(firstRating, secondRating, 1);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondRating, firstRating, 0);

        assertEquals(1672, firstResult.rating());
        assertEquals(299, firstResult.ratingDeviation());

        assertEquals(1328, secondResult.rating());
        assertEquals(299, secondResult.ratingDeviation());
    }

    @Test
    void calculate3() {
        final Rating firstPlayerRating = Rating.fromRepository(1846, 90, 0.45672000);
        final Rating secondPlayerRating = Rating.fromRepository(2843, 30, 0.34543000);

        /** Player with the biggest rating win.*/
        final Rating firstResult = Glicko2RatingCalculator.calculate(firstPlayerRating, secondPlayerRating, 0);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondPlayerRating, firstPlayerRating, 1);

        assertEquals(1846, firstResult.rating());
        assertEquals(120, firstResult.ratingDeviation());

        assertEquals(2843, secondResult.rating());
        assertEquals(67, secondResult.ratingDeviation());

        /** Draw.*/
        final Rating firstResult2 = Glicko2RatingCalculator.calculate(firstPlayerRating, secondPlayerRating, 0.5);
        final Rating secondResult2 = Glicko2RatingCalculator.calculate(secondPlayerRating, firstPlayerRating, 0.5);

        assertEquals(1887, firstResult2.rating());
        assertEquals(120, firstResult2.ratingDeviation());

        assertEquals(2831, secondResult2.rating());
        assertEquals(67, secondResult2.ratingDeviation());

        /** Player with the smallest rating win.*/
        final Rating firstResult3 = Glicko2RatingCalculator.calculate(firstPlayerRating, secondPlayerRating, 1);
        final Rating secondResult3 = Glicko2RatingCalculator.calculate(secondPlayerRating, firstPlayerRating, 0);

        assertEquals(1929, firstResult3.rating());
        assertEquals(121, firstResult3.ratingDeviation());

        assertEquals(2818, secondResult3.rating());
        assertEquals(67, secondResult3.ratingDeviation());
    }

    @Test
    void calculateWithDrawResult() {
        final Rating firstRating = Rating.fromRepository(1483, 144, 0.6);
        final Rating secondRating = Rating.fromRepository(1284, 234, 0.6);

        final Rating firstResult = Glicko2RatingCalculator.calculate(firstRating, secondRating, 0.5);
        final Rating secondResult = Glicko2RatingCalculator.calculate(secondRating, firstRating, 0.5);

        assertEquals(1455, firstResult.rating());
        assertEquals(167, firstResult.ratingDeviation());

        assertEquals(1345, secondResult.rating());
        assertEquals(221, secondResult.ratingDeviation());
    }
}