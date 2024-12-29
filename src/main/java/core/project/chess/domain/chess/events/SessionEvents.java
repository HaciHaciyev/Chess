package core.project.chess.domain.chess.events;

import java.time.LocalDateTime;
import java.util.Objects;

public record SessionEvents(LocalDateTime creationDate,
                            LocalDateTime lastUpdateDate) {

    public SessionEvents {
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(lastUpdateDate);
    }

    public static SessionEvents defaultEvents() {
        return new SessionEvents(LocalDateTime.now(), LocalDateTime.now());
    }
}
