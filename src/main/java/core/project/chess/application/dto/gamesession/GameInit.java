package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.annotations.OptionalArgument;

import java.util.Objects;
import java.util.UUID;

public record GameInit(@OptionalArgument UUID gameId, @OptionalArgument Color color,
                       TimeControllingTYPE time, @OptionalArgument Username nameOfPartner) {

    public GameInit {
        time = Objects.requireNonNullElse(time, TimeControllingTYPE.DEFAULT);
    }
}
