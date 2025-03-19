package core.project.chess.domain.user.events;

import java.time.LocalDateTime;

public final class TokenEvents {

    private final LocalDateTime creationDate;

    private final LocalDateTime expirationDate;

    public static final int EXPIRATION_TIME = 6;

    public TokenEvents(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        this.expirationDate = creationDate.plusMinutes(EXPIRATION_TIME);
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
