package core.project.chess.domain.user.entities;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.util.Glicko2RatingCalculator;
import core.project.chess.domain.commons.value_objects.*;
import core.project.chess.domain.user.value_objects.AccountDates;
import core.project.chess.domain.user.value_objects.PersonalData;
import core.project.chess.domain.user.value_objects.ProfilePicture;

import java.util.*;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.commons.value_objects.GameResult.WHITE_WIN;

public class User {
    private final UUID id;
    private final PersonalData personalData;
    private boolean isEnable;
    private Ratings ratings;
    private final AccountDates accountDates;
    private final Set<UUID> games;
    private final Set<UUID> puzzles;
    private ProfilePicture profilePicture;

    private User(UUID id,
                 PersonalData personalData,
                 boolean isEnable,
                 Ratings ratings,
                 AccountDates accountDates,
                 @Nullable Set<UUID> games,
                 @Nullable Set<UUID> puzzles,
                 @Nullable ProfilePicture profilePicture) {
        this.id = id;
        this.personalData = personalData;
        this.isEnable = isEnable;
        this.ratings = ratings;
        this.accountDates = accountDates;
        this.games = games;
        this.puzzles = puzzles;
        this.profilePicture = profilePicture;
    }

    public static User of(PersonalData personalData) {
        if (personalData == null) throw new IllegalArgumentException("UserProfile cannot be null");
        return new User(
                UUID.randomUUID(),
                personalData,
                false,
                Ratings.defaultRatings(),
                AccountDates.defaultEvents(),
                new HashSet<>(),
                new HashSet<>(),
                null
        );
    }

    /**
     * This method should be called only from the repository layer.
     */
    public static User fromRepository(
            UUID id,
            PersonalData personalData,
            boolean isEnabled,
            Ratings ratings,
            AccountDates events) {
        if (id == null || personalData == null || ratings == null || events == null)
            throw new IllegalArgumentException("Values cannot be null");
        return new User(
                id,
                personalData,
                isEnabled,
                ratings,
                events,
                new HashSet<>(),
                new HashSet<>(),
                null
        );
    }

    public UUID id() {
        return id;
    }

    public String firstname() {
        return personalData.firstname();
    }

    public String surname() {
        return personalData.surname();
    }

    public String username() {
        return personalData.username();
    }

    public String email() {
        return personalData.email();
    }

    public String password() {
        return personalData.password();
    }

    public AccountDates accountEvents() {
        return accountDates;
    }

    @Nullable
    public Set<UUID> games() {
        return new HashSet<>(games);
    }

    @Nullable
    public Set<UUID> puzzles() {
        return new HashSet<>(puzzles);
    }

    public Optional<ProfilePicture> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public Ratings ratings() {
        return ratings;
    }

    public Rating rating() {
        return ratings.rating();
    }

    public Rating bulletRating() {
        return ratings.bulletRating();
    }

    public Rating blitzRating() {
        return ratings.blitzRating();
    }

    public Rating rapidRating() {
        return ratings.rapidRating();
    }

    public Rating puzzlesRating() {
        return ratings.puzzlesRating();
    }

    public void addGame(final UUID game) {
        if (game == null)
            throw new IllegalArgumentException("game id can`t be null");
        games.add(game);
    }

    public void addPuzzle(final UUID puzzle) {
        if (puzzle == null)
            throw new IllegalArgumentException("puzzle id can`t be null");
        puzzles.add(puzzle);
    }

    public void enable() {
        if (isEnable) throw new IllegalStateException("You can`t activate already activated account");
        this.isEnable = true;
    }

    public void changeRating(final RatingUpdate ratingUpdate) {
        Objects.requireNonNull(ratingUpdate);

        final Color color;
        if (ratingUpdate.whitePlayerID().equals(id)) color = WHITE;
        else if (ratingUpdate.blackPlayerID().equals(this.id)) color = Color.BLACK;
        else throw new IllegalArgumentException("This user did not participate in this game.");

        final double result = getResult(ratingUpdate.gameResult(), color);
        final Rating opponentRating =  color == WHITE ? ratingUpdate.blackPlayerRating() : ratingUpdate.whitePlayerRating();

        switch (ratingUpdate.ratingType()) {
            case CLASSIC -> {
                Rating newRating = Glicko2RatingCalculator.calculate(this.ratings.rating(), opponentRating, result);
                this.ratings = Ratings.newRating(this.ratings, newRating);
            }

            case BULLET -> {
                Rating newBulletRating = Glicko2RatingCalculator.calculate(this.ratings.bulletRating(), opponentRating, result);
                this.ratings = Ratings.newBulletRating(this.ratings, newBulletRating);
            }

            case BLITZ -> {
                Rating newBlitzRating = Glicko2RatingCalculator.calculate(this.ratings.blitzRating(), opponentRating, result);
                this.ratings = Ratings.newBlitzRating(this.ratings, newBlitzRating);
            }

            case RAPID -> {
                Rating newRapidRating = Glicko2RatingCalculator.calculate(this.ratings.rapidRating(), opponentRating, result);
                this.ratings = Ratings.newRapidRating(this.ratings, newRapidRating);
            }
        }
    }

    public void changeRating(final RatingUpdateOnPuzzle ratingUpdate) {
        Objects.requireNonNull(ratingUpdate);

        final boolean doNotMatch = !ratingUpdate.playerID().equals(this.id);
        if (doNotMatch) {
            throw new IllegalArgumentException("Puzzle does not belong to this user");
        }

        final double result = ratingUpdate.gameResult() == PuzzleStatus.SOLVED ? 1 : -1;
        Rating newPuzzlesRating = Glicko2RatingCalculator.calculate(this.ratings.puzzlesRating(), ratingUpdate.puzzleRating(), result);
        this.ratings = Ratings.newPuzzlesRating(this.ratings, newPuzzlesRating);
    }

    private double getResult(final GameResult gameResult, final Color color) {
        if (gameResult.equals(GameResult.DRAW)) return 0.5;
        if (gameResult.equals(WHITE_WIN)) return color.equals(WHITE) ? 1 : 0;
        return color.equals(BLACK) ? 1 : 0;
    }

    public void setProfilePicture(ProfilePicture picture) {
        Objects.requireNonNull(picture);
        this.profilePicture = picture;
    }

    public void deleteProfilePicture() {
        this.profilePicture = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return isEnable == that.isEnable &&
                Objects.equals(id, that.id) &&
                Objects.equals(personalData, that.personalData) &&
                Objects.equals(ratings, that.ratings) &&
                Objects.equals(accountDates, that.accountDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personalData, isEnable, ratings, accountDates);
    }

    @Override
    public String toString() {
        String enables;
        if (Boolean.TRUE.equals(isEnable)) {
            enables = "enable";
        } else {
            enables = "disable";
        }

        return String.format("""
               UserAccount: %s {
                    Firstname: %s,
                    Surname: %s,
                    Username : %s,
                    Email : %s,
                    Is enable : %s,
                    Rating : %f,
                    Bullet rating: %f,
                    Blitz rating: %f,
                    Rapid rating: %f,
                    Puzzles rating: %f,
                    Creation date : %s,
                    Last updated date : %s
               }
               """,
                id,
                personalData.firstname(),
                personalData.surname(),
                personalData.username(),
                personalData.email(),
                enables,
                ratings.rating().rating(),
                ratings.bulletRating().rating(),
                ratings.blitzRating().rating(),
                ratings.rapidRating().rating(),
                ratings.puzzlesRating().rating(),
                accountDates.creationDate().toString(),
                accountDates.lastUpdateDate().toString()
        );
    }
}
