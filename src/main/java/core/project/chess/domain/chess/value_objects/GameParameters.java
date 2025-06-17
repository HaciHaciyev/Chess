package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.enumerations.Color;

import java.util.Objects;

public record GameParameters(@Nullable Color color,
                             @Nullable ChessGame.Time time,
                             @Nullable String FEN,
                             Boolean isCasualGame,
                             @Nullable String PGN) {
    public GameParameters {
        isCasualGame = Objects.requireNonNullElse(isCasualGame, false);
    }
}
