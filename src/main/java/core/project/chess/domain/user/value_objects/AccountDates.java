package core.project.chess.domain.user.value_objects;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccountDates(LocalDateTime creationDate,
                           LocalDateTime lastUpdateDate) {

    public AccountDates {
        if (Objects.isNull(creationDate) || Objects.isNull(lastUpdateDate)) {
            throw new IllegalArgumentException("Creation and last update dates cannot be null");
        }
    }

    public static AccountDates defaultEvents() {
        return new AccountDates(LocalDateTime.now(), LocalDateTime.now());
    }
}
