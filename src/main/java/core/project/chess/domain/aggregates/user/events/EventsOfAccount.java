package core.project.chess.domain.aggregates.user.events;

import java.time.LocalDateTime;
import java.util.Objects;

public record EventsOfAccount(LocalDateTime creationDate,
                              LocalDateTime lastUpdateDate) {

    public EventsOfAccount {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            throw new IllegalArgumentException("The creationDate and lastUpdateDate must not be null");
        }
    }

    public static EventsOfAccount defaultEvents() {
        return new EventsOfAccount(LocalDateTime.now(), LocalDateTime.now());
    }
}
