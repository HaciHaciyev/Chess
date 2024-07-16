package core.project.chess.domain.aggregates.user.entities;

import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.Token;

import java.util.Objects;
import java.util.UUID;

public record EmailConfirmationToken(UUID tokenId, Token token,
                                     TokenEvents tokenEvents,
                                     /**@OneToOne*/ UserAccount userAccount) {

    public EmailConfirmationToken {
        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenEvents);
        Objects.requireNonNull(userAccount);
    }

    public boolean isExpired() {
        return tokenEvents.isExpired();
    }
}
