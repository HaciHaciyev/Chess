package core.project.chess.domain.user.events;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccountEvents(LocalDateTime creationDate,
                            LocalDateTime lastUpdateDate) {

    public AccountEvents {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            throw new IllegalArgumentException("Creation and last update dates cannot be null");
        }
    }

    public static AccountEvents defaultEvents() {
        return new AccountEvents(LocalDateTime.now(), LocalDateTime.now());
    }
}
