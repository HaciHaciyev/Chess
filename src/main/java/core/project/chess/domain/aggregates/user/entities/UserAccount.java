package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.chess.entities.GameOfChess;
import core.project.chess.domain.aggregates.user.events.EventsOfAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Slf4j
@Getter
@AllArgsConstructor
public class UserAccount implements UserDetails {
    private final transient @NonNull UUID id;
    private final transient @NonNull Username username;
    private final transient @NonNull Email email;
    private transient @NonNull Password password;
    private transient @NonNull Password passwordConfirm;
    private transient @NonNull Rating rating;
    private transient @NonNull Boolean isEnable;
    private final transient @NonNull EventsOfAccount eventsOfAccount;
    private final transient @NonNull /**@ManyToMany*/ Set<UserAccount> partners;
    private final transient @NonNull /**@ManyToMany*/ Set<GameOfChess> games;

    public static Builder builder() {
        return new Builder();
    }

    public void addPartner(UserAccount partner) {
        partners.add(partner);
    }

    public void removePartner(UserAccount partner) {
        partners.remove(partner);
    }

    public void addGame(GameOfChess game) {
        games.add(game);
    }

    public void removeGame(GameOfChess game) {
        games.remove(game);
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
               Objects.equals(eventsOfAccount, that.eventsOfAccount);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(email);
        result = 31 * result + Objects.hashCode(rating);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(passwordConfirm);
        result = 31 * result + Objects.hashCode(eventsOfAccount);
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
                eventsOfAccount.creationDate().toString(),
                eventsOfAccount.lastUpdateDate().toString());
    }

    public static class Builder {
        private UUID id;
        private Username username;
        private Email email;
        private Password password;
        private Password passwordConfirm;
        private /**Optional*/ Rating rating;
        private /**Optional*/ Boolean isEnable;
        private /**Optional*/ EventsOfAccount eventsOfAccount;

        private Builder() {}

        public Builder id(final @NonNull UUID id) {
            this.id = id;
            return this;
        }

        public Builder username(final @NonNull Username username) {
            this.username = username;
            return this;
        }

        public Builder email(final @NonNull Email email) {
            this.email = email;
            return this;
        }

        public Builder password(final @NonNull Password password) {
            this.password = password;
            return this;
        }

        public Builder passwordConfirm(final @NonNull Password passwordConfirm) {
            this.passwordConfirm = passwordConfirm;
            return this;
        }

        public Builder rating(final @Nullable Rating rating) {
            this.rating = rating;
            return this;
        }

        public Builder enable(final @Nullable Boolean enable) {
            isEnable = enable;
            return this;
        }

        public Builder eventsOfAccount(final @Nullable EventsOfAccount eventsOfAccount) {
            this.eventsOfAccount = eventsOfAccount;
            return this;
        }

        public UserAccount build() {
            boolean defaultAccount = rating == null && isEnable == null && eventsOfAccount == null;
            if (defaultAccount) log.info("New account created.");

            boolean allOptionalValuesNotNull = rating != null && isEnable != null && eventsOfAccount != null;
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
                                                .eventsOfAccount(EventsOfAccount.defaultEvents())
                                                .build();
                        """;

                throw new IllegalArgumentException(infoAboutUserAccountCreation);
            }

            short defaultRating = 1400;
            return new UserAccount(
                    this.id, this.username, this.email,
                    this.password, this.passwordConfirm,
                    Objects.requireNonNullElse(this.rating, new Rating(defaultRating)),
                    Objects.requireNonNullElse(this.isEnable, Boolean.FALSE),
                    Objects.requireNonNullElse(this.eventsOfAccount, EventsOfAccount.defaultEvents()),
                    new HashSet<>(), new HashSet<>()
            );
        }
    }
}
