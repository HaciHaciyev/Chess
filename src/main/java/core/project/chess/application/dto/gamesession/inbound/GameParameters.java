package core.project.chess.application.dto.gamesession.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;

public record GameParameters(@Nullable Color color, ChessGame.TimeControllingTYPE timeControllingTYPE, LocalDateTime creationTime) {

    public long waitingTime() {
        return Duration.between(creationTime, LocalDateTime.now()).toMinutes();
    }
}
