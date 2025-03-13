package core.project.chess.domain.user.entities;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.user.events.AccountEvents;
import core.project.chess.domain.user.util.Glicko2RatingCalculator;
import core.project.chess.domain.user.value_objects.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.enumerations.GameResult.WHITE_WIN;

public class UserAccount {
    private final UUID id;
    private final Firstname firstname;
    private final Surname surname;
    private final Username username;
    private final Email email;
    private final Password password;
    private UserRole userRole;
    private boolean isEnable;
    private Rating rating;
    private Rating bulletRating;
    private Rating blitzRating;
    private Rating rapidRating;
    private Rating puzzlesRating;
    private final AccountEvents accountEvents;
    private final Set<UserAccount> partners;
    private final Set<ChessGame> games;
    private final Set<Puzzle> puzzles;
    private ProfilePicture profilePicture;

    private UserAccount(UUID id, Firstname firstname, Surname surname, Username username, Email email, Password password,
                       UserRole userRole, boolean isEnable, Rating rating, Rating bulletRating, Rating blitzRating, Rating rapidRating,
                       Rating puzzlesRating, AccountEvents accountEvents, Set<UserAccount> partners, Set<ChessGame> games,
                       Set<Puzzle> puzzles, ProfilePicture profilePicture) {
        this.id = id;
        this.firstname = firstname;
        this.surname = surname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.userRole = userRole;
        this.isEnable = isEnable;
        this.rating = rating;
        this.bulletRating = bulletRating;
        this.blitzRating = blitzRating;
        this.rapidRating = rapidRating;
        this.puzzlesRating = puzzlesRating;
        this.accountEvents = accountEvents;
        this.partners = partners;
        this.games = games;
        this.puzzles = puzzles;
        this.profilePicture = profilePicture;
    }

    public static UserAccount of(Firstname firstname, Surname surname, Username username, Email email, Password password) {
        Objects.requireNonNull(firstname);
        Objects.requireNonNull(surname);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        return new UserAccount(
                UUID.randomUUID(), firstname, surname, username, email, password, UserRole.NONE, false,
                Rating.defaultRating(), Rating.defaultRating(), Rating.defaultRating(), Rating.defaultRating(), Rating.defaultRating(),
                AccountEvents.defaultEvents(), new HashSet<>(), new HashSet<>(), new HashSet<>(), ProfilePicture.defaultProfilePicture()
        );
    }

    /**
     * this method is used to call only from repository
     */
    public static UserAccount fromRepository(UUID id, Firstname firstname, Surname surname, Username username, Email email,
                                             Password password, UserRole userRole, boolean enabled, Rating rating, Rating bulletRating,
                                             Rating blitzRating, Rating rapidRating, Rating puzzlesRating,
                                             AccountEvents events, ProfilePicture profilePicture) {

        Objects.requireNonNull(id);
        Objects.requireNonNull(firstname);
        Objects.requireNonNull(surname);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
        Objects.requireNonNull(userRole);
        Objects.requireNonNull(rating);
        Objects.requireNonNull(bulletRating);
        Objects.requireNonNull(blitzRating);
        Objects.requireNonNull(rapidRating);
        Objects.requireNonNull(puzzlesRating);
        Objects.requireNonNull(events);

        return new UserAccount(
                id, firstname, surname, username, email, password, userRole, enabled,
                rating, bulletRating, blitzRating, rapidRating, puzzlesRating,
                events, new HashSet<>(), new HashSet<>(), new HashSet<>(),
                Objects.requireNonNullElseGet(profilePicture, ProfilePicture::defaultProfilePicture)
        );
    }

    public UUID getId() {
        return id;
    }

    public Firstname getFirstname() {
        return firstname;
    }

    public Surname getSurname() {
        return surname;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
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

    public Set<ChessGame> getGames() {
        return games;
    }

    public Set<Puzzle> getPuzzles() {
        return puzzles;
    }

    public ProfilePicture getProfilePicture() {
        return profilePicture;
    }

    public boolean isEnabled() {
        return isEnable;
    }

    public Rating getRating() {
        return rating;
    }

    public Rating getBulletRating() {
        return bulletRating;
    }

    public Rating getBlitzRating() {
        return blitzRating;
    }

    public Rating getRapidRating() {
        return rapidRating;
    }

    public Rating getPuzzlesRating() {
        return puzzlesRating;
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
            throw new IllegalArgumentException("This user and puzzle is do not match.");
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
        if (chessGame.getPlayerForWhite().getId().equals(this.id)) {
            color = WHITE;
        }
        else if (chessGame.getPlayerForBlack().getId().equals(this.id)) {
            color = Color.BLACK;
        }
        else {
            throw new IllegalArgumentException("This user did not participate in this game.");
        }

        final double result = getResult(chessGame.gameResult().get(), color);
        final UserAccount opponent =  color.equals(WHITE) ? chessGame.getPlayerForBlack() : chessGame.getPlayerForWhite();

        switch (chessGame.getTime()) {
            case DEFAULT, CLASSIC -> this.rating = Glicko2RatingCalculator.calculate(this.rating, opponent.getRating(), result);
            case BULLET -> this.bulletRating = Glicko2RatingCalculator.calculate(this.bulletRating, opponent.getBlitzRating(), result);
            case BLITZ -> this.blitzRating = Glicko2RatingCalculator.calculate(this.blitzRating, opponent.getBlitzRating(), result);
            case RAPID -> this.rapidRating = Glicko2RatingCalculator.calculate(this.rapidRating, opponent.getRapidRating(), result);
        }
    }

    public void changeRating(final Puzzle puzzle) {
        Objects.requireNonNull(puzzle);
        if (!puzzle.isEnded()) {
            throw new IllegalArgumentException("Puzzle is not ended.");
        }

        final boolean doNotMatch = !puzzle.player().getId().equals(this.id);
        if (doNotMatch) {
            throw new IllegalArgumentException("This user and puzzle is do not match.");
        }

        final double result = puzzle.isSolved() ? 1 : -1;
        this.puzzlesRating = Glicko2RatingCalculator.calculate(this.rating, puzzle.rating(), result);
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
        this.profilePicture = ProfilePicture.defaultProfilePicture();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;

        return isEnable == that.isEnable && Objects.equals(id, that.id) && Objects.equals(username, that.username) &&
                Objects.equals(email, that.email) && Objects.equals(password, that.password) && userRole == that.userRole &&
                Objects.equals(rating, that.rating) && Objects.equals(accountEvents, that.accountEvents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, username, email, password, userRole, isEnable, rating, accountEvents
        );
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
                username.username(),
                email.email(),
                userRole,
                enables,
                rating.rating(),
                bulletRating.rating(),
                blitzRating.rating(),
                rapidRating.rating(),
                puzzlesRating.rating(),
                accountEvents.creationDate().toString(),
                accountEvents.lastUpdateDate().toString()
        );
    }
}
