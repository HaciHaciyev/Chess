package core.project.chess.application.model;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.value_objects.Color;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record InboundGameParameters(@Nullable Color color, ChessGame.TimeControllingTYPE timeControllingTYPE, LocalDateTime creationTime) {

    public InboundGameParameters {
        Objects.requireNonNull(timeControllingTYPE);
        Objects.requireNonNull(creationTime);
    }

    public long waitingTime() {
        return Duration.between(creationTime, LocalDateTime.now()).toMinutes();
    }
}
