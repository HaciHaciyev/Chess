package core.project.chess.domain.user.value_objects;

import java.time.LocalDateTime;

public final class TokenDates {

    private final LocalDateTime creationDate;

    private final LocalDateTime expirationDate;

    public static final int EXPIRATION_TIME = 6;

    public TokenDates(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        this.expirationDate = creationDate.plusMinutes(EXPIRATION_TIME);
    }

    public LocalDateTime creationDate() {
        return creationDate;
    }

    public LocalDateTime expirationDate() {
        return expirationDate;
    }

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDateTime.now());
    }
}
