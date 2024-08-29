package core.project.chess.application.model;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.value_objects.Color;
import core.project.chess.infrastructure.utilities.OptionalArgument;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record GameParameters(@OptionalArgument Color color, ChessGame.TimeControllingTYPE timeControllingTYPE, LocalDateTime creationTime) {

    public GameParameters {
        Objects.requireNonNull(timeControllingTYPE);
        Objects.requireNonNull(creationTime);
    }

    public long waitingTime() {
        return Duration.between(creationTime, LocalDateTime.now()).toMinutes();
    }
}
