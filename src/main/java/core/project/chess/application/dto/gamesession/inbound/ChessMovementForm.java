package core.project.chess.application.dto.gamesession.inbound;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Piece;
import jakarta.annotation.Nullable;

public record ChessMovementForm(Coordinate from, Coordinate to, @Nullable Piece inCaseOfPromotion) { }
