package core.project.chess.domain.aggregates.chess.events;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
public record GameEvents(LocalDateTime creationDate,
                         LocalDateTime lastUpdateDate) {

    public GameEvents {
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(lastUpdateDate);
    }

    public static GameEvents defaultEvents() {
        return new GameEvents(LocalDateTime.now(), LocalDateTime.now());
    }
}
