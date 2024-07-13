package core.project.chess.domain.aggregates.user.events;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

public record EventsOfAccount(Timestamp creationDate,
                              Timestamp lastUpdateDate) {

    public EventsOfAccount {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            // TODO
        }
    }

    public static EventsOfAccount defaultEvents() {
        return new EventsOfAccount(Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));
    }
}
