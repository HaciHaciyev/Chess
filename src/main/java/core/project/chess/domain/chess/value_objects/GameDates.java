package core.project.chess.domain.chess.value_objects;

import java.time.LocalDateTime;
import java.util.Objects;

public record GameDates(LocalDateTime creationDate,
                        LocalDateTime lastUpdateDate) {

    public GameDates {
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(lastUpdateDate);
    }

    public static GameDates defaultEvents() {
        return new GameDates(LocalDateTime.now(), LocalDateTime.now());
    }
}
