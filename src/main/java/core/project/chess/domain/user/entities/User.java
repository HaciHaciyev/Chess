package core.project.chess.domain.user.entities;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.events.AccountEvents;
import core.project.chess.domain.user.util.Glicko2RatingCalculator;
import core.project.chess.domain.user.value_objects.PersonalData;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.domain.user.value_objects.Rating;
import core.project.chess.domain.user.value_objects.Ratings;
import jakarta.annotation.Nullable;

import java.util.*;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.enumerations.GameResult.WHITE_WIN;

public class User {
    private final UUID id;
    private final PersonalData personalData;
    private boolean isEnable;
    private Ratings ratings;
    private final AccountEvents accountEvents;
    private final Set<User> partners;
    private final Set<ChessGame> games;
    private final Set<Puzzle> puzzles;
    private ProfilePicture profilePicture;

    private User(UUID id,
                 PersonalData personalData,
                 boolean isEnable,
                 Ratings ratings,
                 AccountEvents accountEvents,
                 @Nullable Set<User> partners,
                 @Nullable Set<ChessGame> games,
                 @Nullable Set<Puzzle> puzzles,
                 @Nullable ProfilePicture profilePicture) {
        this.id = id;
        this.personalData = personalData;
        this.isEnable = isEnable;
        this.ratings = ratings;
        this.accountEvents = accountEvents;
        this.partners = partners;
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
                AccountEvents.defaultEvents(),
                new HashSet<>(),
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
            AccountEvents events) {
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

    public boolean isEnable() {
        return isEnable;
    }

    public AccountEvents accountEvents() {
        return accountEvents;
    }

    @Nullable
    public Set<ChessGame> games() {
        return games;
    }

    @Nullable
    public Set<Puzzle> puzzles() {
        return puzzles;
    }

    public Set<User> partners() {
        return new HashSet<>(partners);
    }

    public Optional<ProfilePicture> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    public boolean isEnabled() {
        return isEnable;
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

    public void addGame(final ChessGame game) {
        Objects.requireNonNull(game);
        games.add(game);
    }

    public void addPuzzle(final Puzzle puzzle) {
        final boolean doNotMatch = !puzzle.player().id().equals(this.id);
        if (doNotMatch) {
            throw new IllegalArgumentException("Puzzle does not belong to this user");
        }

        puzzles.add(puzzle);
    }

    public void enable() {
        if (isEnable) throw new IllegalStateException("You can`t activate already activated account");
        this.isEnable = true;
    }

    public void changeRating(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("Game result is empty.");
        }

        final Color color;
        if (chessGame.whitePlayer().id().equals(this.id)) {
            color = WHITE;
        }
        else if (chessGame.blackPlayer().id().equals(this.id)) {
            color = Color.BLACK;
        }
        else {
            throw new IllegalArgumentException("This user did not participate in this game.");
        }

        final double result = getResult(chessGame.gameResult().get(), color);
        final User opponent =  color.equals(WHITE) ? chessGame.blackPlayer() : chessGame.whitePlayer();

        switch (chessGame.time()) {
            case DEFAULT, CLASSIC -> {
                Rating newRating = Glicko2RatingCalculator.calculate(this.ratings.rating(), opponent.rating(), result);
                this.ratings = Ratings.newRating(this.ratings, newRating);
            }

            case BULLET -> {
                Rating newBulletRating = Glicko2RatingCalculator.calculate(this.ratings.bulletRating(), opponent.bulletRating(), result);
                this.ratings = Ratings.newBulletRating(this.ratings, newBulletRating);
            }

            case BLITZ -> {
                Rating newBlitzRating = Glicko2RatingCalculator.calculate(this.ratings.blitzRating(), opponent.blitzRating(), result);
                this.ratings = Ratings.newBlitzRating(this.ratings, newBlitzRating);
            }

            case RAPID -> {
                Rating newRapidRating = Glicko2RatingCalculator.calculate(this.ratings.rapidRating(), opponent.rapidRating(), result);
                this.ratings = Ratings.newRapidRating(this.ratings, newRapidRating);
            }
        }
    }

    public void changeRating(final Puzzle puzzle) {
        Objects.requireNonNull(puzzle);
        if (!puzzle.isEnded()) {
            throw new IllegalArgumentException("Puzzle is not ended.");
        }

        final boolean doNotMatch = !puzzle.player().id().equals(this.id);
        if (doNotMatch) {
            throw new IllegalArgumentException("Puzzle does not belong to this user");
        }

        final double result = puzzle.isSolved() ? 1 : -1;
        Rating newPuzzlesRating = Glicko2RatingCalculator.calculate(this.ratings.puzzlesRating(), puzzle.rating(), result);
        this.ratings = Ratings.newPuzzlesRating(this.ratings, newPuzzlesRating);
    }

    private double getResult(final GameResult gameResult, final Color color) {
        if (gameResult.equals(GameResult.DRAW)) {
            return 0.5;
        }

        if (gameResult.equals(WHITE_WIN)) {
            return color.equals(WHITE) ? 1 : 0;
        }

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
                Objects.equals(accountEvents, that.accountEvents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personalData, isEnable, ratings, accountEvents);
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
                accountEvents.creationDate().toString(),
                accountEvents.lastUpdateDate().toString()
        );
    }
}
