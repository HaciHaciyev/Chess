package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.Token;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailConfirmationToken {
    private final UUID tokenId;
    private final Token token;
    private final TokenEvents tokenEvents;
    private @Getter(AccessLevel.PRIVATE) Boolean isConfirmed;
    private final /**@OneToOne*/ UserAccount userAccount;

    public static Builder builder() {
        return new Builder();
    }

    public boolean isExpired() {
        return tokenEvents.isExpired();
    }

    public boolean isConfirmed() {
        return isConfirmed.equals(Boolean.TRUE);
    }

    public void confirm() {
        if (isConfirmed.equals(Boolean.TRUE)) {
            throw new IllegalStateException("Email confirmation token is already confirmed");
        }

        isConfirmed = Boolean.TRUE;
    }

    public static class Builder {
        private UUID tokenId;
        private UserAccount userAccount;
        private /**Optional*/ Token token;
        private /**Optional*/ TokenEvents tokenEvents;
        private /**Optional*/ Boolean isConfirmed;

        private Builder() {}

        public Builder tokenId(final UUID tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder userAccount(final UserAccount userAccount) {
            this.userAccount = userAccount;
            return this;
        }

        public Builder token(final @Nullable Token token) {
            this.token = token;
            return this;
        }

        public Builder tokenEvents(final @Nullable TokenEvents tokenEvents) {
            this.tokenEvents = tokenEvents;
            return this;
        }

        public Builder confirmed(final @Nullable Boolean confirmed) {
            isConfirmed = confirmed;
            return this;
        }

        public EmailConfirmationToken build() {
            Objects.requireNonNull(tokenId);
            Objects.requireNonNull(userAccount);

            boolean allUsed = token != null && tokenEvents != null && isConfirmed != null;
            if (allUsed) {
                log.info("EmailConfirmation token was recreated.");
            }

            if (!allUsed) {
                String doc = """
                        You have to different ways for create this entity:
                       
                        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                                        .tokenId(...)
                                        .userAccount(...)
                                        .build();
                        
                        EmailConfirmationToken emailConfirmationTokenSecond = EmailConfirmationToken.builder()
                                       .tokenId(...)
                                       .userAccount(...)
                                       .setToken(...)
                                       .tokenEvents(...)
                                       .confirmed(...)
                                       .build();
                        
                        The first method must be used when creating a new token and therefore
                        can only be used in controllers to verify the email addresses of new users.
                        
                        The second method is used to obtain a token that already exists in the database,
                        for example in repositories.
                        """;

                log.info(doc);
                throw new IllegalStateException(doc);
            }

            return new EmailConfirmationToken(
                    tokenId, Objects.requireNonNullElse(token, Token.createToken()),
                    Objects.requireNonNullElse(tokenEvents, new TokenEvents(LocalDateTime.now())),
                    Objects.requireNonNullElse(isConfirmed,false), userAccount
            );
        }
    }
}
