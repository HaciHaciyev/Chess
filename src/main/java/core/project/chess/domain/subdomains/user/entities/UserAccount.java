package core.project.chess.domain.subdomains.user.entities;

import core.project.chess.domain.subdomains.chess.entities.ChessGame;
import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.GameResult;
import core.project.chess.domain.subdomains.user.events.AccountEvents;
import core.project.chess.domain.subdomains.user.util.Glicko2RatingCalculator;
import core.project.chess.domain.subdomains.user.value_objects.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static core.project.chess.domain.subdomains.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.subdomains.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.subdomains.chess.enumerations.GameResult.WHITE_WIN;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccount {
    private final UUID id;
    private final Username username;
    private final Email email;
    private final Password password;
    private UserRole userRole;
    private boolean isEnable;
    private Rating rating;
    private final AccountEvents accountEvents;
    private final Set<UserAccount> partners;
    private final Set<ChessGame> games;
    private ProfilePicture profilePicture;

    public static UserAccount of(Username username, Email email, Password password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        return new UserAccount(
                UUID.randomUUID(), username, email, password, UserRole.NONE, false,
                Rating.defaultRating(), AccountEvents.defaultEvents(), new HashSet<>(), new HashSet<>(),
                ProfilePicture.defaultProfilePicture()
        );
    }

    /**
     * this method is used to call only from repository
     */
    public static UserAccount fromRepository(UUID id, Username username, Email email, Password password, UserRole userRole,
                                             boolean enabled, Rating rating, AccountEvents events, ProfilePicture profilePicture) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
        Objects.requireNonNull(userRole);
        Objects.requireNonNull(rating);
        Objects.requireNonNull(events);

        return new UserAccount(
                id, username, email, password, userRole, enabled, rating, events,
                new HashSet<>(), new HashSet<>(), Objects.requireNonNullElseGet(profilePicture, ProfilePicture::defaultProfilePicture)
        );
    }

    public boolean isEnabled() {
        return isEnable;
    }

    public Rating getRating() {
        return Rating.fromRepository(this.rating.rating(), this.rating.ratingDeviation(), this.rating.volatility());
    }

    public Set<UserAccount> getPartners() {
        return new HashSet<>(partners);
    }

    public void addGame(final ChessGame game) {
        Objects.requireNonNull(game);
        games.add(game);
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
        final Rating opponentRating = color.equals(WHITE) ? chessGame.getPlayerForBlack().getRating() : chessGame.getPlayerForWhite().getRating();

        this.rating = Glicko2RatingCalculator.calculate(this.rating, opponentRating, result);
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
                accountEvents.creationDate().toString(),
                accountEvents.lastUpdateDate().toString()
        );
    }
}
