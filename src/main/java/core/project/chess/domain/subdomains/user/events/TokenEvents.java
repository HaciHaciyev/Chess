package core.project.chess.domain.subdomains.user.events;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class TokenEvents {

    private final LocalDateTime creationDate;

    private final LocalDateTime expirationDate;

    public TokenEvents(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        this.expirationDate = creationDate.plusMinutes(6);
    }

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDateTime.now());
    }
}
