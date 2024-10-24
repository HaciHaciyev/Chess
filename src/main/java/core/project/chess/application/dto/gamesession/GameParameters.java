package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.infrastructure.utilities.annotations.OptionalArgument;

import java.time.Duration;
import java.time.LocalDateTime;

public record GameParameters(@OptionalArgument Color color, ChessGame.TimeControllingTYPE timeControllingTYPE, LocalDateTime creationTime) {

    public long waitingTime() {
        return Duration.between(creationTime, LocalDateTime.now()).toMinutes();
    }
}
