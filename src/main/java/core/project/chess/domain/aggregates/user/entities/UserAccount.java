package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.value_objects.*;
import core.project.chess.infrastructure.utilities.Glicko2RatingCalculator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccount {
    private final UUID id;
    private final Username username;
    private final Email email;
    private final Password password;
    private final UserRole userRole;
    private boolean isEnable;
    private Rating rating;
    private final AccountEvents accountEvents;
    private final /**@ManyToMany*/ Set<UserAccount> partners;
    private final /**@ManyToMany*/ Set<ChessGame> games;

    public static UserAccount of(Username username, Email email, Password password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        short defaultRating = 1500;

        return new UserAccount(
                UUID.randomUUID(), username, email, password, UserRole.NONE, false,
                new Rating(defaultRating), AccountEvents.defaultEvents(), new HashSet<>(), new HashSet<>()
        );
    }

    /**
     * this method is used to call only from repository
     */
    public static UserAccount fromRepository(
            UUID id, Username username, Email email, Password password, UserRole userRole, boolean enabled, Rating rating, AccountEvents events
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
        Objects.requireNonNull(userRole);
        Objects.requireNonNull(rating);
        Objects.requireNonNull(events);

        return new UserAccount(id, username, email, password, userRole, enabled, rating, events, new HashSet<>(), new HashSet<>());
    }

    public boolean isEnable() {
        return isEnable;
    }

    public Rating getRating() {
        final short ratingForReturn = this.rating.rating();
        return new Rating(ratingForReturn);
    }

    public void addPartner(final UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.add(partner);
    }

    public void removePartner(final UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.remove(partner);
    }

    public void addGame(final ChessGame game) {
        Objects.requireNonNull(game);
        games.add(game);
    }

    public void removeGame(final ChessGame game) {
        Objects.requireNonNull(game);
        games.remove(game);
    }

    public void enable() {
        this.isEnable = true;
    }

    public void changeRating(final ChessGame chessGame) {
        Objects.requireNonNull(chessGame);
        validateRatingChanging(chessGame);

        final short updatedRating = Glicko2RatingCalculator.calculateRating(chessGame);
        this.rating = new Rating(updatedRating);
    }

    private void validateRatingChanging(ChessGame chessGame) {
        if (chessGame.gameResult().isEmpty()) {
            throw new IllegalArgumentException("The game is not ended for rating calculation.");
        }

        short secondPlayerRating = 0;

        final boolean isWhitePlayer = chessGame.getPlayerForWhite().getId().equals(this.id);
        if (isWhitePlayer) {
            secondPlayerRating = chessGame.getPlayerForBlackRating().rating();
        }

        final boolean blackPlayer = chessGame.getPlayerForBlack().getId().equals(this.id);
        if (blackPlayer) {
            secondPlayerRating = chessGame.getPlayerForWhiteRating().rating();
        }

        if (secondPlayerRating == 0) throw new IllegalArgumentException("Invalid method usage, check documentation.");
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
                    Rating : %d,
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
