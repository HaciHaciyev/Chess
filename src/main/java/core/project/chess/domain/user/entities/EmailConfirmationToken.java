package core.project.chess.domain.user.entities;

import core.project.chess.domain.user.value_objects.Token;
import core.project.chess.domain.user.value_objects.TokenDates;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class EmailConfirmationToken {
    private final UUID tokenId;
    private final Token token;
    private final TokenDates tokenDates;
    private boolean isConfirmed;
    private final User user;

    private EmailConfirmationToken(
            UUID tokenId,
            Token token,
            TokenDates tokenDates,
            boolean isConfirmed,
            User user) {

        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenDates);
        Objects.requireNonNull(user);

        this.tokenId = tokenId;
        this.token = token;
        this.tokenDates = tokenDates;
        this.isConfirmed = isConfirmed;
        this.user = user;
    }

    public static EmailConfirmationToken createToken(final User user) {
        return new EmailConfirmationToken(
                UUID.randomUUID(), Token.createToken(), new TokenDates(LocalDateTime.now()), false, user
        );
    }

    public static EmailConfirmationToken fromRepository(
            UUID tokenId, Token token, TokenDates tokenDates, Boolean isConfirmed, User user
    ) {
        return new EmailConfirmationToken(tokenId, token, tokenDates, isConfirmed, user);
    }

    public UUID tokenID() {
        return tokenId;
    }

    public Token token() {
        return token;
    }

    public TokenDates tokenEvents() {
        return tokenDates;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public User user() {
        return user;
    }

    public boolean isExpired() {
        return tokenDates.isExpired();
    }

    public void confirm() {
        if (isConfirmed) throw new IllegalStateException("This token has already been confirmed");
        isConfirmed = true;
    }
}
