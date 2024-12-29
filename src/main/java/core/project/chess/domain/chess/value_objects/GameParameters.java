package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import jakarta.annotation.Nullable;

public record GameParameters(@Nullable Color color,
                             @Nullable ChessGame.Time time,
                             @Nullable String FEN,
                             boolean isCasualGame) {}
