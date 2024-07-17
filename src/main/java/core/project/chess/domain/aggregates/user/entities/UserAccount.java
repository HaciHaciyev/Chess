package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.chess.entities.GameOfChess;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Slf4j
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccount
        implements UserDetails {
    private final UUID id;
    private final Username username;
    private final Email email;
    private Password password;
    private Password passwordConfirm;
    private Rating rating;
    private @Getter(AccessLevel.PRIVATE) Boolean isEnable;
    private final AccountEvents accountEvents;
    private final /**@ManyToMany*/ Set<UserAccount> partners;
    private final /**@ManyToMany*/ Set<GameOfChess> games;

    public static Builder builder() {
        return new Builder();
    }

    public void addPartner(UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.add(partner);
    }

    public void removePartner(UserAccount partner) {
        Objects.requireNonNull(partner);
        partners.remove(partner);
    }

    public void addGame(GameOfChess game) {
        Objects.requireNonNull(game);
        games.add(game);
    }

    public void removeGame(GameOfChess game) {
        Objects.requireNonNull(game);
        games.remove(game);
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
               Objects.equals(passwordConfirm, that.passwordConfirm) &&
               Objects.equals(accountEvents, that.accountEvents);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(email);
        result = 31 * result + Objects.hashCode(rating);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(passwordConfirm);
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

    public static class Builder {
        private UUID id;
        private Username username;
        private Email email;
        private Password password;
        private Password passwordConfirm;
        private /**Optional*/ Rating rating;
        private /**Optional*/ Boolean isEnable;
        private /**Optional*/ AccountEvents accountEvents;

        private Builder() {}

        public Builder id(final UUID id) {
            Objects.requireNonNull(id);
            this.id = id;
            return this;
        }

        public Builder username(final Username username) {
            Objects.requireNonNull(username);
            this.username = username;
            return this;
        }

        public Builder email(final Email email) {
            Objects.requireNonNull(email);
            this.email = email;
            return this;
        }

        public Builder password(final Password password) {
            Objects.requireNonNull(password);
            this.password = password;
            return this;
        }

        public Builder passwordConfirm(final Password passwordConfirm) {
            Objects.requireNonNull(passwordConfirm);
            this.passwordConfirm = passwordConfirm;
            return this;
        }

        public Builder rating(final Rating rating) {
            Objects.requireNonNull(rating);
            this.rating = rating;
            return this;
        }

        public Builder enable(final @Nullable Boolean enable) {
            isEnable = enable;
            return this;
        }

        public Builder accountEvents(final @Nullable AccountEvents accountEvents) {
            this.accountEvents = accountEvents;
            return this;
        }

        public UserAccount build() {
            boolean defaultAccount = rating == null && isEnable == null && accountEvents == null;
            if (defaultAccount) log.info("New account created.");

            boolean allOptionalValuesNotNull = rating != null && isEnable != null && accountEvents != null;
            if (allOptionalValuesNotNull) log.info("An existing account is used.");

            if (!defaultAccount && !allOptionalValuesNotNull) {
                String infoAboutUserAccountCreation = """
                        There was an attempt to incorrectly create a UserAccount using the Builder.
                        You have two ways to create account :
                            First used when account is new and have not
                            a values that need to be calculated in future
                            like Rating or values that can be created by default like isEnable or Events
                            and this way of entity creation need to be used only in Controller layers.
                            Example :
                        
                                UserAccount defaultUserAccount = UserAccount.builder()
                                                .id(UUID.randomUUID())
                                                .username(new Username(User))
                                                .email(new Email(email@gmail.com))
                                                .password(new Password(password))
                                                .passwordConfirm(new Password(password))
                                                .build();
                        
                            Second can be used throughout the project
                            and need to be used for existed account and contains all values.
                            Example :
                        
                                UserAccount userAccount = UserAccount.builder()
                                                .id(UUID.randomUUID())
                                                .username(new Username(Older user))
                                                .email(new Email(email@gmail.com))
                                                .password(new Password(password))
                                                .passwordConfirm(new Password(password))
                                                .rating(new Rating(rating))
                                                .enable(true)
                                                .accountEvents(EventsOfAccount.defaultEvents())
                                                .build();
                        """;

                log.info(infoAboutUserAccountCreation);
                throw new IllegalArgumentException(infoAboutUserAccountCreation);
            }

            short defaultRating = 1400;
            return new UserAccount(
                    this.id, this.username, this.email,
                    this.password, this.passwordConfirm,
                    Objects.requireNonNullElse(this.rating, new Rating(defaultRating)),
                    Objects.requireNonNullElse(this.isEnable, Boolean.FALSE),
                    Objects.requireNonNullElse(this.accountEvents, AccountEvents.defaultEvents()),
                    new HashSet<>(), new HashSet<>()
            );
        }
    }
}
