package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.Token;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

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

        private Builder() {}

        public Builder setTokenId(final UUID tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder setUserAccount(final UserAccount userAccount) {
            this.userAccount = userAccount;
            return this;
        }

        public EmailConfirmationToken build() {
            Objects.requireNonNull(tokenId);
            Objects.requireNonNull(userAccount);

            return new EmailConfirmationToken(
                    tokenId, Token.createToken(), new TokenEvents(LocalDateTime.now()), false , userAccount
            );
        }
    }
}
