package core.project.chess.domain.user.entities;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.events.AccountEvents;
import core.project.chess.domain.user.util.Glicko2RatingCalculator;
import core.project.chess.domain.user.value_objects.*;
import jakarta.annotation.Nullable;

import java.util.*;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.enumerations.GameResult.WHITE_WIN;

public class UserAccount {
    private final UUID id;
    private final PersonalData personalData;
    private UserRole userRole;
    private boolean isEnable;
    private Ratings ratings;
    private final AccountEvents accountEvents;
    private final @Nullable Set<UserAccount> partners;
    private final @Nullable Set<ChessGame> games;
    private final @Nullable Set<Puzzle> puzzles;
    private @Nullable ProfilePicture profilePicture;

    private UserAccount(UUID id,
                        PersonalData personalData,
                        UserRole userRole,
                        boolean isEnable, Ratings ratings,
                        AccountEvents accountEvents,
                        @Nullable Set<UserAccount> partners,
                        @Nullable Set<ChessGame> games,
                        @Nullable Set<Puzzle> puzzles,
                        @Nullable ProfilePicture profilePicture) {
        this.id = id;
        this.personalData = personalData;
        this.userRole = userRole;
        this.isEnable = isEnable;
        this.ratings = ratings;
        this.accountEvents = accountEvents;
        this.partners = partners;
        this.games = games;
        this.puzzles = puzzles;
        this.profilePicture = profilePicture;
    }

    public static UserAccount of(PersonalData personalData) {
        if (personalData == null) throw new IllegalArgumentException("UserProfile cannot be null");

        return new UserAccount(
                UUID.randomUUID(), personalData, UserRole.NONE, false, Ratings.defaultRatings(),
                AccountEvents.defaultEvents(), new HashSet<>(), new HashSet<>(), new HashSet<>(), null
        );
    }

    /**
     * this method is used to call only from repository
     */
    public static UserAccount fromRepository(UUID id,
                                             PersonalData personalData,
                                             UserRole userRole,
                                             boolean enabled,
                                             Ratings ratings,
                                             AccountEvents events) {
        if (id == null || personalData == null || userRole == null || ratings == null || events == null)
            throw new IllegalArgumentException("Values cannot be null");

        return new UserAccount(
                id, personalData, userRole, enabled, ratings, events, new HashSet<>(), new HashSet<>(), new HashSet<>(), null
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFirstname() {
        return personalData.firstname();
    }

    public String getSurname() {
        return personalData.surname();
    }

    public String getUsername() {
        return personalData.username();
    }

    public String getEmail() {
        return personalData.email();
    }

    public String getPassword() {
        return personalData.password();
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public AccountEvents getAccountEvents() {
        return accountEvents;
    }

    @Nullable
    public Set<ChessGame> getGames() {
        return games;
    }

    @Nullable
    public Set<Puzzle> getPuzzles() {
        return puzzles;
    }

    public Optional<ProfilePicture> getProfilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    public boolean isEnabled() {
        return isEnable;
    }

    public Rating getRating() {
        return ratings.rating();
    }

    public Rating getBulletRating() {
        return ratings.bulletRating();
    }

    public Rating getBlitzRating() {
        return ratings.blitzRating();
    }

    public Rating getRapidRating() {
        return ratings.rapidRating();
    }

    public Rating getPuzzlesRating() {
        return ratings.puzzlesRating();
    }

    public Set<UserAccount> getPartners() {
        return new HashSet<>(partners);
    }

    public void addGame(final ChessGame game) {
        Objects.requireNonNull(game);
        games.add(game);
    }

    public void addPuzzle(final Puzzle puzzle) {
        final boolean doNotMatch = !puzzle.player().getId().equals(this.id);
        if (doNotMatch) {
            throw new IllegalArgumentException("Puzzle does not belong to this user");
        }

        puzzles.add(puzzle);
    }

    public void enable() {
        this.isEnable = true;
        if (userRole.equals(UserRole.NONE)) {
            userRole = UserRole.ROLE_USER;
        }
    }

    public void changeRating(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("Game result is empty.");
        }

        final Color color;
        if (chessGame.getWhitePlayer().getId().equals(this.id)) {
            color = WHITE;
        }
        else if (chessGame.getBlackPlayer().getId().equals(this.id)) {
            color = Color.BLACK;
        }
        else {
            throw new IllegalArgumentException("This user did not participate in this game.");
        }

        final double result = getResult(chessGame.gameResult().get(), color);
        final UserAccount opponent =  color.equals(WHITE) ? chessGame.getBlackPlayer() : chessGame.getWhitePlayer();

        switch (chessGame.getTime()) {
            case DEFAULT, CLASSIC -> {
                Rating newRating = Glicko2RatingCalculator.calculate(this.ratings.rating(), opponent.getRating(), result);
                this.ratings = Ratings.newRating(this.ratings, newRating);
            }

            case BULLET -> {
                Rating newBulletRating = Glicko2RatingCalculator.calculate(this.ratings.bulletRating(), opponent.getBulletRating(), result);
                this.ratings = Ratings.newBulletRating(this.ratings, newBulletRating);
            }

            case BLITZ -> {
                Rating newBlitzRating = Glicko2RatingCalculator.calculate(this.ratings.blitzRating(), opponent.getBlitzRating(), result);
                this.ratings = Ratings.newBlitzRating(this.ratings, newBlitzRating);
            }

            case RAPID -> {
                Rating newRapidRating = Glicko2RatingCalculator.calculate(this.ratings.rapidRating(), opponent.getRapidRating(), result);
                this.ratings = Ratings.newRapidRating(this.ratings, newRapidRating);
            }
        }
    }

    public void changeRating(final Puzzle puzzle) {
        Objects.requireNonNull(puzzle);
        if (!puzzle.isEnded()) {
            throw new IllegalArgumentException("Puzzle is not ended.");
        }

        final boolean doNotMatch = !puzzle.player().getId().equals(this.id);
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
        UserAccount that = (UserAccount) o;
        return isEnable == that.isEnable &&
                Objects.equals(id, that.id) &&
                Objects.equals(personalData, that.personalData) &&
                userRole == that.userRole &&
                Objects.equals(ratings, that.ratings) &&
                Objects.equals(accountEvents, that.accountEvents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personalData, userRole, isEnable, ratings, accountEvents);
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
                    User role : %s,
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
                userRole,
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
