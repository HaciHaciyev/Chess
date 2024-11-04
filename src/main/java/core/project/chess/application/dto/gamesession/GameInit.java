package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

public record GameInit(@Nullable UUID gameId, @Nullable Color color,
                       TimeControllingTYPE time, @Nullable Username nameOfPartner) {

    public GameInit {
        time = Objects.requireNonNullElse(time, TimeControllingTYPE.DEFAULT);
    }
}
