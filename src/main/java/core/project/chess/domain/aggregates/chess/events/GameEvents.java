package core.project.chess.domain.aggregates.chess.events;

import java.time.LocalDateTime;
import java.util.Objects;

public record GameEvents(LocalDateTime creationDate,
                         LocalDateTime lastUpdateDate) {

    public GameEvents {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            // TODO
        }
    }

    public static GameEvents defaultEvents() {
        return new GameEvents(LocalDateTime.now(), LocalDateTime.now());
    }
}
