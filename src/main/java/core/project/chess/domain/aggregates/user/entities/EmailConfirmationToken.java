package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.Token;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class EmailConfirmationToken {
    private final UUID tokenId;
    private final Token token;
    private final TokenEvents tokenEvents;
    private boolean isConfirmed;
    private final /**@OneToOne*/ UserAccount userAccount;

    private EmailConfirmationToken(UUID tokenId, Token token, TokenEvents tokenEvents, boolean isConfirmed, UserAccount userAccount) {
        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenEvents);
        Objects.requireNonNull(userAccount);

        this.tokenId = tokenId;
        this.token = token;
        this.tokenEvents = tokenEvents;
        this.isConfirmed = isConfirmed;
        this.userAccount = userAccount;
    }

    public static EmailConfirmationToken createToken(final UserAccount userAccount) {
        return new EmailConfirmationToken(
                UUID.randomUUID(), Token.createToken(), new TokenEvents(LocalDateTime.now()), false, userAccount
        );
    }

    public static EmailConfirmationToken fromRepository(
            final UUID tokenId, final Token token, final TokenEvents tokenEvents, final Boolean isConfirmed, final UserAccount userAccount
    ) {
        return new EmailConfirmationToken(tokenId, token, tokenEvents, isConfirmed, userAccount);
    }

    public boolean isExpired() {
        return tokenEvents.isExpired();
    }

    public void confirm() {
        if (isConfirmed) {
            throw new IllegalStateException("Email confirmation token is already confirmed");
        }

        isConfirmed = true;
    }
}
