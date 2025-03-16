package core.project.chess.domain.user.events;

import java.time.LocalDateTime;

public final class TokenEvents {

    private final LocalDateTime creationDate;

    private final LocalDateTime expirationDate;

    public TokenEvents(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        this.expirationDate = creationDate.plusMinutes(6);
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDateTime.now());
    }
}
