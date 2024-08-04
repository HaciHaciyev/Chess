package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Slf4j
@Getter
public class UserAccount implements UserDetails {
    private final UUID id;
    private final Username username;
    private final Email email;
    private Password password;
    private Rating rating;
    private @Getter(AccessLevel.PRIVATE) Boolean isEnable;
    private final AccountEvents accountEvents;
    private final /**@ManyToMany*/ Set<UserAccount> partners;
    private final /**@ManyToMany*/ Set<ChessGame> games;

    private UserAccount(
            UUID id, Username username, Email email, Password password, Rating rating, Boolean isEnable,
            AccountEvents accountEvents, Set<UserAccount> partners, Set<ChessGame> games
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
        Objects.requireNonNull(rating);
        Objects.requireNonNull(isEnable);
        Objects.requireNonNull(accountEvents);
        Objects.requireNonNull(partners);
        Objects.requireNonNull(games);

        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.rating = rating;
        this.isEnable = isEnable;
        this.accountEvents = accountEvents;
        this.partners = partners;
        this.games = games;
    }

    public static UserAccount of(Username username, Email email, Password password) {
        short defaultRating = 1400;
        return new UserAccount(
                UUID.randomUUID(), username, email, password, new Rating(defaultRating), Boolean.FALSE,
                AccountEvents.defaultEvents(), new HashSet<>(), new HashSet<>()
        );
    }

    /**
     * this method is used to call from repository
     */
    public static UserAccount fromRepository(
            UUID id, Username username, Email email, Password password, Rating rating, boolean enabled, AccountEvents events
    ) {
        return new UserAccount(
                id, username, email, password, rating, enabled, events, new HashSet<>(), new HashSet<>()
        );
    }

    public void addPartner(UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.add(partner);
    }

    public void removePartner(UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.remove(partner);
    }

    public void addGame(ChessGame game) {
        Objects.requireNonNull(game);
        games.add(game);
    }

    public void removeGame(ChessGame game) {
        Objects.requireNonNull(game);
        games.remove(game);
    }

    public void changePassword(Password password) {
        Objects.requireNonNull(password);
        this.password = password;
    }

    public void changeRating(Rating rating) {
        Objects.requireNonNull(rating);
        this.rating = rating;
    }

    public void enable() {
        this.isEnable = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password.password();
    }

    @Override
    public String getUsername() {
        return username.username();
    }

    @Override
    public boolean isEnabled() {
        return isEnable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (partners.size() != ((UserAccount) o).partners.size()) return false;

        UserAccount that = (UserAccount) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(username, that.username) &&
               Objects.equals(email, that.email) &&
               Objects.equals(rating, that.rating) &&
               Objects.equals(password, that.password) &&
               Objects.equals(accountEvents, that.accountEvents);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(email);
        result = 31 * result + Objects.hashCode(rating);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(accountEvents);
        return result;
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
               \nUserAccount: %s {
                    user name : %s,
                    email : %s,
                    rating : %d,
                    is enable : %s,
                    creation date : %s,
                    last updated date : %s
               }
               """,
                id,
                username.username(),
                email.email(),
                rating.rating(),
                enables,
                accountEvents.creationDate().toString(),
                accountEvents.lastUpdateDate().toString());
    }
}
