package core.project.chess.domain.aggregates.chess.events;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
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
