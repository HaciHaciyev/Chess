package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.entities.Puzzle;

public class PuzzleRatingWindow {
    private final double minRating;
    private final double maxRating;

    public PuzzleRatingWindow(double rating) {
        if (rating <= 0) throw new IllegalArgumentException("Rating can`t be equal or below zero");
        double minRating = rating - Puzzle.USER_RATING_WINDOW;
        this.minRating = minRating > 0 ? minRating : 1;
        this.maxRating  = rating + Puzzle.USER_RATING_WINDOW;
    }

    public double minRating() {
        return minRating;
    }

    public double maxRating() {
        return maxRating;
    }
}
