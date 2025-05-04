package core.project.chess.domain.user.entities;

import core.project.chess.domain.user.events.TokenEvents;
import core.project.chess.domain.user.value_objects.Token;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class EmailConfirmationToken {
    private final UUID tokenId;
    private final Token token;
    private final TokenEvents tokenEvents;
    private boolean isConfirmed;
    private final User user;

    private EmailConfirmationToken(
            UUID tokenId,
            Token token,
            TokenEvents tokenEvents,
            boolean isConfirmed,
            User user) {

        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenEvents);
        Objects.requireNonNull(user);

        this.tokenId = tokenId;
        this.token = token;
        this.tokenEvents = tokenEvents;
        this.isConfirmed = isConfirmed;
        this.user = user;
    }

    public static EmailConfirmationToken createToken(final User user) {
        return new EmailConfirmationToken(
                UUID.randomUUID(), Token.createToken(), new TokenEvents(LocalDateTime.now()), false, user
        );
    }

    public static EmailConfirmationToken fromRepository(
            UUID tokenId, Token token, TokenEvents tokenEvents, Boolean isConfirmed, User user
    ) {
        return new EmailConfirmationToken(tokenId, token, tokenEvents, isConfirmed, user);
    }

    public UUID tokenID() {
        return tokenId;
    }

    public Token token() {
        return token;
    }

    public TokenEvents tokenEvents() {
        return tokenEvents;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public User user() {
        return user;
    }

    public boolean isExpired() {
        return tokenEvents.isExpired();
    }

    public void confirm() {
        if (isConfirmed) throw new IllegalStateException("This token has already been confirmed");
        isConfirmed = true;
    }
}
