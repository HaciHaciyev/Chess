package core.project.chess.application.dto.chess;

import core.project.chess.domain.subdomains.chess.entities.ChessGame;
import core.project.chess.domain.subdomains.chess.enumerations.Color;
import jakarta.annotation.Nullable;

public record GameParameters(@Nullable Color color,
                             @Nullable ChessGame.TimeControllingTYPE timeControllingTYPE,
                             @Nullable String FEN) {}
