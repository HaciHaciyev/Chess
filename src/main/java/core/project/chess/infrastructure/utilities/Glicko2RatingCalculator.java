package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;

import java.util.Objects;

import static core.project.chess.domain.aggregates.chess.enumerations.GameResult.DRAW;
import static core.project.chess.domain.aggregates.chess.enumerations.GameResult.WHITE_WIN;
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

    private static final double ε = 0.000001;

    private Glicko2RatingCalculator() {}

    /**
     * Calculates the updated Glicko-2 ratings for two players based on the outcome of a chess game.
     * <p>
     * The method retrieves the current ratings, rating deviations, and volatilities for both players
     * involved in the game. It then applies the Glicko-2 rating calculation algorithm to determine
     * the new ratings for both players based on the game result.
     *
     * @param chessGame The ChessGame object representing the game played between two players.
     *                  This object must not be null and must contain a valid game result.
     *
     * @return A Pair containing the updated ratings for the white and black players, respectively.
     *         The first element of the Pair corresponds to the white player's updated rating,
     *         and the second element corresponds to the black player's updated rating.
     *
     * @throws NullPointerException if the provided chessGame is null.
     * @throws IllegalArgumentException if the game result of the chessGame is empty.
     *
     * @see ChessGame
     * @see UserAccount
     * @see Rating
     *
     * <p>
     * The Glicko-2 rating system is designed to provide a more dynamic and accurate representation
     * of a player's skill level compared to traditional Elo ratings. It takes into account:
     * <ul>
     *     <li><strong>Rating:</strong> The player's skill level.</li>
     *     <li><strong>Rating Deviation:</strong> A measure of the uncertainty in the player's rating.
     *         A lower deviation indicates a more stable rating.</li>
     *     <li><strong>Volatility:</strong> A measure of the degree of expected fluctuation in a player's rating.
     *         Higher volatility indicates that a player's performance is more unpredictable.</li>
     * </ul>
     * The algorithm adjusts the ratings based on the outcome of the game, taking into account the
     * relative strengths of the players and the uncertainty in their ratings.
     * </p>
     */
    public static Pair<Rating, Rating> calculateRating(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("Game result is empty.");
        }

        final UserAccount playerForWhite = chessGame.getPlayerForWhite();
        final UserAccount playerForBlack = chessGame.getPlayerForBlack();

        final double whiteRating = playerForWhite.getRating().rating();
        final double whiteRatingDeviation = playerForWhite.getRating().ratingDeviation();
        final double whiteVolatility = playerForWhite.getRating().volatility();

        final double blackRating = playerForBlack.getRating().rating();
        final double blackRatingDeviation = playerForBlack.getRating().ratingDeviation();
        final double blackVolatility = playerForBlack.getRating().volatility();

        final double resultForWhite = whitePlayerResult(chessGame.gameResult().orElseThrow());

        return calculate(whiteRating, whiteRatingDeviation, whiteVolatility, blackRating, blackRatingDeviation, blackVolatility, resultForWhite);
    }

    private static double whitePlayerResult(final GameResult gameResult) {
        if (gameResult.equals(DRAW)) {
            return 0.5;
        }

        return gameResult.equals(WHITE_WIN) ? 1 : 0;
    }

    private static double opposite(final double resultForWhite) {
        if (resultForWhite == 0.5) {
            return 0.5;
        }

        return resultForWhite == 1 ? -1 : 1;
    }

    /**
     * Calculates the updated ratings and rating deviations for two players using the Glicko-2 rating system.
     * <p>
     * This method takes the current ratings, rating deviations, and volatilities of two players (white and black)
     * along with the result of the game for the white player. It computes the new ratings and rating deviations
     * based on the Glicko-2 algorithm, which accounts for the uncertainty in player ratings.
     * <p>
     * The calculations involve several intermediate steps, including:
     * - Converting ratings and deviations to the Glicko-2 scale.
     * - Calculating the rating dispersion for both players.
     * - Computing the delta values for both players based on the game results.
     * - Updating the volatility for both players.
     * - Finally, calculating the new ratings and deviations, converting them back to the original scale.
     *
     * @param whiteRating              The current rating of the white player.
     * @param whiteRatingDeviation     The current rating deviation of the white player.
     * @param whiteVolatility          The current volatility of the white player.
     * @param blackRating              The current rating of the black player.
     * @param blackRatingDeviation     The current rating deviation of the black player.
     * @param blackVolatility          The current volatility of the black player.
     * @param resultForWhite           The result of the game for the white player (1 for win, 0.5 for draw, 0 for loss).
     * @return                         A Pair containing the updated ratings and rating deviations for both players.
     *                                 The first element of the Pair is the updated rating for the white player,
     *                                 and the second element is the updated rating for the black player.
     */
    private static Pair<Rating, Rating> calculate(double whiteRating, double whiteRatingDeviation, double whiteVolatility,
                                                  double blackRating, double blackRatingDeviation, double blackVolatility, double resultForWhite) {

        // μ.
        final double whitePLayerRatingInGlicko2Scale = (whiteRating - 1500) / 173.7178;

        // φ.
        final double whitePlayerRatingDeviationInGlicko2Scale = whiteRatingDeviation / 173.7178;

        // σ.
        final double whitePlayerRatingVolatilityInGlicko2Scale = whiteVolatility;

        // s.
        final double resultOfGameForWhitePlayer = resultForWhite;

        // μ.
        final double blackPLayerRatingInGlicko2Scale = (blackRating - 1500) / 173.7178;

        // φ.
        final double blackPlayerRatingDeviationInGlicko2Scale = blackRatingDeviation / 173.7178;

        // σ.
        final double blackPlayerRatingVolatilityInGlicko2Scale = blackVolatility;

        // s.
        final double resultOfGameForBlackPlayer = opposite(resultForWhite);


        // v.
        final double ratingDispersionOfWhitePlayer = ratingDispersion(
                whitePLayerRatingInGlicko2Scale,
                blackPLayerRatingInGlicko2Scale,
                blackPlayerRatingDeviationInGlicko2Scale
        );


        // Δ.
        final double whitePlayerDelta = delta(
                ratingDispersionOfWhitePlayer,
                whitePLayerRatingInGlicko2Scale,
                blackPLayerRatingInGlicko2Scale,
                blackPlayerRatingDeviationInGlicko2Scale,
                resultOfGameForBlackPlayer
        );


        // a - a = ln(o^2).
        final double whitePlayerRatingVariance = log(
                pow(whitePlayerRatingVolatilityInGlicko2Scale, 2)
        );


        // v.
        final double ratingDispersionOfBlackPlayer = ratingDispersion(
                blackPLayerRatingInGlicko2Scale,
                whitePLayerRatingInGlicko2Scale,
                whitePlayerRatingDeviationInGlicko2Scale
        );

        // Δ.
        final double blackPlayerDelta = delta(
                ratingDispersionOfBlackPlayer,
                whitePLayerRatingInGlicko2Scale,
                whitePlayerRatingDeviationInGlicko2Scale,
                resultOfGameForWhitePlayer,
                resultOfGameForBlackPlayer
        );

        // a - a = ln(o^2).
        final double blackPlayerRatingVariance = log(
                pow(blackPlayerRatingVolatilityInGlicko2Scale, 2)
        );


        // σ′.
        final double resultWhiteVolatility = computeNewVolatility(
                /* r.*/ whitePLayerRatingInGlicko2Scale,
                /* a.*/ whitePlayerRatingVariance,
                /* v.*/ ratingDispersionOfWhitePlayer,
                /* Δ.*/ whitePlayerDelta,
                /* φ.*/ whitePlayerRatingDeviationInGlicko2Scale
        );


        // σ′.
        final double resultBlackVolatility = computeNewVolatility(
                /* r.*/ blackPLayerRatingInGlicko2Scale,
                /* a.*/ blackPlayerRatingVariance,
                /* v.*/ ratingDispersionOfBlackPlayer,
                /* Δ.*/ blackPlayerDelta,
                /* φ.*/ blackPlayerRatingDeviationInGlicko2Scale
        );


        // φ′.
        final double newWhiteRatingDeviation = updateRatingDeviation(
                whitePlayerRatingDeviationInGlicko2Scale,
                resultWhiteVolatility,
                ratingDispersionOfWhitePlayer
        );


        // φ′.
        final double newBlackRatingDeviation = updateRatingDeviation(
                blackPlayerRatingDeviationInGlicko2Scale,
                resultBlackVolatility,
                ratingDispersionOfBlackPlayer
        );


        // μ′.
        final double newWhiteRating = updateRating(
                whitePLayerRatingInGlicko2Scale,
                newWhiteRatingDeviation,
                blackPLayerRatingInGlicko2Scale,
                blackPlayerRatingDeviationInGlicko2Scale,
                resultOfGameForBlackPlayer
        );


        // μ′.
        final double newBlackRating = updateRating(
                blackPLayerRatingInGlicko2Scale,
                newBlackRatingDeviation,
                whitePLayerRatingInGlicko2Scale,
                whitePlayerRatingDeviationInGlicko2Scale,
                resultOfGameForWhitePlayer
        );


        // RD1
        final double resultOfWhitePlayerRatingDeviation = 173.7178 * newWhiteRatingDeviation;
        final double resultOfBlackPlayerRatingDeviation = 173.7178 * newBlackRatingDeviation;

        // R1
        final double resultOfWhitePlayerRating = 173.7178 * newWhiteRating + 1500;
        final double resultOfBlackPlayerRating = 173.7178 * newBlackRating + 1500;


        return Pair.of(
                Rating.fromRepository(resultOfWhitePlayerRating, resultOfWhitePlayerRatingDeviation, resultWhiteVolatility),
                Rating.fromRepository(resultOfBlackPlayerRating, resultOfBlackPlayerRatingDeviation, resultBlackVolatility)
        );
    }

    /**
     * Updates the rating based on the given parameters.
     * <p>
     * This method calculates a new rating using the provided parameters, which include
     * the player's current rating (μ), a factor (φ1), the opponent's rating (μj),
     * the opponent's factor (φj), and the score (sj) achieved in the match.
     * <p>
     * Formula:
     * μ′ = μ + φ′^2 * g(φj) * {sj − E(μ, μj , φj)}
     *
     * @param μ     The current rating of the player.
     * @param φ1    The factor associated with the current player.
     * @param μj    The rating of the opponent.
     * @param φj    The factor associated with the opponent.
     * @param sj    The score achieved by the player in the match.
     * @return      The updated rating as a double value.
     */
    private static double updateRating(final double μ, final double φ1, final double μj, final double φj, final double sj) {
        final double firstHalf = μ * pow(φ1, 2);
        final double secondHalf = adjustmentFunction(φ1) * (sj - expectedScoreFunction(μ, μj, φj));
        return firstHalf * secondHalf;
    }

    /**
     * Updates the rating deviation (φ*) based on the current rating deviation (φ),
     * the volatility (σ1), and the variance (v).
     * <p>
     * φ′ = 1 / √(1 / φ∗2) + (1 / v)
     *
     * @param φ     The current rating deviation (φ).
     * @param σ1    The volatility of the rating (σ1).
     * @param v     The variance of the rating (v).
     * @return The updated rating deviation (φ*) as a double value.
     */
    private static double updateRatingDeviation(final double φ, final double σ1, final double v) {
        // φ*
        final double preRatingDeviation = sqrt(
                pow(φ, 2) + pow(σ1, 2)
        );

        return 1 /
                sqrt(
                    (1 / pow(preRatingDeviation, 2) + (1 / v)
                )
        );
    }

    /**
     * Computes the new volatility based on the provided parameters using an iterative method.
     * <p>
     * Formula:
     * 1. Let a = ln(σ2).
     * 2. ε = 0.000001.
     * 3. A = a = ln(σ2).
     * 4. If ∆^2 > φ^2 + v, then set B = ln(∆2 − φ2 − v).
     *    If ∆^2 ≤ φ^2 + v, then perform the following iteration:
     *      (i) Let k = 1
     *      (ii) If f (a − kτ ) < 0, then
     *      Set k ← k + 1
     *      Go to (ii).
     *      and set B = a − kτ . The values A and B are chosen to bracket ln(σ′^2), and
     *      the remainder of the algorithm iteratively narrows this bracket.
     * 5. While |B − A| > ε, carry out the following steps.
     *      (a) Let C = A + (A − B)fA/(fB − fA), and let fC = f (C).
     *      (b) If fC fB ≤ 0, then set A ← B and fA ← fB ; otherwise, just set fA ← fA/2.
     *      (c) Set B ← C and fB ← fC .
     *      (d) Stop if |B − A| ≤ ε. Repeat the above three steps otherwise.
     * 6. Once |B − A| ≤ ε, set σ′ ← eA/2.
     *
     * @param r     The parameter representing the rate (e.g., interest rate).
     * @param a     The natural logarithm of the initial volatility squared (ln(σ²)).
     * @param v     The variance or another relevant parameter in the computation.
     * @param delta The parameter representing the change in a variable (e.g., price change).
     * @param φ     The parameter representing a specific value related to the volatility.
     * @return The computed new volatility (σ') as a double value.
     */
    private static double computeNewVolatility(final double r, final double a, final double v, final double delta, final double φ) {

        double A = a;
        double fA = f(A, r, a, v, delta, φ);

        double B = 0;
        if (pow(delta, 2) > pow(φ, 2) + v) {
            B = log(
                    pow(delta, 2) - pow(φ, 2) - v
            );
        }

        if (pow(delta, 2) <= pow(φ, 2) + v) {
            double k = 1;
            final double fk = f(
                    (a - (k * r)), r, a, v, delta, φ
            );

            if (fk < 0) {
                k += 1;
            }

            B = a - (k * r);
        }

        double fB = f(B, r, a, v, delta, φ);

        while (abs(B - A) > ε) {
            final double C = A + (A - B) * fA / (fB - fA);
            final double fC = f(C, r, a, v, delta, φ);

            if (fC * fB < 0) {
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

    /**
     * Computes the value of the function f based on the provided parameters.
     * <p>
     * Formula:
     * <p>
     * f (x) = (e^x(∆^2 − φ^2 − v − ex) / 2 * (φ^2 + v + e^x)^2) − ((x − a) / τ 2)
     *
     * @param x     The input value for which the function is computed.
     * @param r     The parameter representing the rate (e.g., interest rate).
     * @param a     The natural logarithm of the initial volatility squared (ln(σ²)).
     * @param v     The variance or another relevant parameter in the computation.
     * @param delta The parameter representing the change in a variable (e.g., price change).
     * @param φ     The parameter representing a specific value related to the volatility.
     * @return The computed value of the function f as a double.
     */
    private static double f(final double x, final double r, final double a, final double v, final double delta, final double φ) {
        final double firstUpperLineOfFormula = pow(E, x) * (pow(delta, 2) - pow(φ, 2) - v - pow(E, x));
        final double firstLowerLineOfFormula = 2 * pow(
                pow(φ, 2) + v + pow(E, x), 2
        );

        final double firstHalfOfTheFormula = firstUpperLineOfFormula / firstLowerLineOfFormula;

        final double secondHalfOfTheFormula = (x - a) / pow(r, 2);

        return firstHalfOfTheFormula - secondHalfOfTheFormula;
    }

    /**
     * Calculates the change in rating (delta) for a player after a match.
     * <p>
     * This method is used in the Glicko-2 rating system to determine how much
     * a player's rating should be adjusted based on the outcome of a match against
     * an opponent. The delta value is influenced by the expected score, the actual
     * score of the player, and the variance of the player's rating.
     * <p>
     * The formula for delta is:
     * <p>
     * Δ = g(φj)² * (s - E(μ, μj, φj)) * v
     * <p>
     * where:
     * - Δ: The change in rating (delta).
     * - g(φj): The adjustment function for the opponent's rating deviation.
     * - s: The actual score of the player (1 for a win, 0.5 for a draw, 0 for a loss).
     * - E(μ, μj, φj): The expected score of the player against the opponent.
     * - v: The variance of the player's rating.
     *
     * @param v The variance of the player's rating.
     * @param μ The rating of the player.
     * @param μj The rating of the opponent.
     * @param φj The rating deviation of the opponent.
     * @param sj The actual score of the player (1 for win, 0.5 for draw, 0 for loss).
     * @return The change in rating (delta) for the player after the match.
     */
    private static double delta(double v, final double μ, final double μj, final double φj, final double sj) {
        double delta = pow(adjustmentFunction(φj), 2) * (sj - expectedScoreFunction(μ, μj, φj));
        delta *= v;

        return delta;
    }

    /**
     * Calculates the rating dispersion of a player.
     * <p>
     * Formula:
     * v = [ Σ (g(φj)² * E(μ, μj, φj) * (1 - E(μ, μj, φj))) ]⁻¹
     * actually we don`t use loop because of only two players :
     * v = [ (g(φj)² * E(μ, μj, φj) * (1 - E(μ, μj, φj))) ]⁻¹
     * <p>
     * where:
     * - v: rating dispersion
     * - g(φj): function that adjusts the rating deviation of opponent j
     * - E(μ, μj, φj): expected score of the match between the player and opponent j
     * - μ: player's rating
     * - μj: opponent's rating
     * - φj: opponent's rating deviation
     *
     * @param μ Player's rating
     * @param μj Opponent's rating
     * @param φj Opponent's rating deviation
     * @return Rating dispersion
     */
    private static double ratingDispersion(final double μ, final double μj, final double φj) {
        final double E = expectedScoreFunction(μ, μj, φj);

        return 1 / (
                pow(adjustmentFunction(φj), 2) * E * (1 - E)
        );
    }

    /**
     * Function g(φ).
     * Calculates the adjustment function for the rating deviation.
     * <p>
     * This function is used in the Glicko-2 rating system to adjust the impact
     * of a player's rating based on their rating deviation (φ). The adjustment
     * function helps to account for the uncertainty in a player's skill level.
     * <p>
     * The formula for the adjustment function is:
     * <p>
     * g(φ) = 1 / √(1 + (3 * φ²) / π²)
     * <p>
     * where:
     * - φ: The rating deviation of the player.
     *
     * @param φ The rating deviation of the player.
     * @return The adjusted value based on the rating deviation.
     */
    private static double adjustmentFunction(double φ) {
        return 1 / sqrt(
                1 + (3 * pow(φ, 2)) / pow(PI, 2)
        );
    }

    /**
     * Function E(μ, μj, φj).
     * Calculates the expected score of a player in a match against an opponent.
     * <p>
     * This function is used in the Glicko-2 rating system to determine the expected
     * outcome of a match based on the ratings of the player and the opponent, as well
     * as the opponent's rating deviation. The expected score is a value between 0 and 1,
     * representing the probability of the player winning the match.
     * <p>
     * The formula for the expected score is:
     * <p>
     * E(μ, μj, φj) = 1 / (1 + exp(-g(φj) * (μ - μj)))
     * <p>
     * where:
     * - μ: The rating of the player whose expected score is being calculated.
     * - μj: The rating of the opponent.
     * - φj: The rating deviation of the opponent.
     *
     * @param μ The rating of the player.
     * @param μj The rating of the opponent.
     * @param φj The rating deviation of the opponent.
     * @return The expected score of the player against the opponent, a value between 0 and 1.
     */
    private static double expectedScoreFunction(final double μ, final double μj, final double φj) {
        return 1 /
                (1 +
                        exp(-adjustmentFunction(φj) * (μ - μj))
                );
    }
}
