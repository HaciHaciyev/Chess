package core.project.chess.domain.aggregates.chess.events;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

public record GameEvents(Timestamp creationDate,
                         Timestamp lastUpdateDate) {

    public GameEvents() {
        this(Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));
    }

    public GameEvents {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            // TODO
        }
    }
}
