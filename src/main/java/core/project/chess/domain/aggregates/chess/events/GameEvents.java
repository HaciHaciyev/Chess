package core.project.chess.domain.aggregates.chess.events;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
public record GameEvents(LocalDateTime creationDate,
                         LocalDateTime lastUpdateDate) {

    public GameEvents {
        creationDate = Objects.requireNonNullElseGet(creationDate, () -> {
            log.error("creationDate is null, falling back to default value");
            return LocalDateTime.now();
        });

        lastUpdateDate = Objects.requireNonNullElseGet(lastUpdateDate, () -> {
            log.error("lastUpdateDate is null, falling back to default value");
            return LocalDateTime.now();
        });
    }

    public static GameEvents defaultEvents() {
        return new GameEvents(LocalDateTime.now(), LocalDateTime.now());
    }
}
