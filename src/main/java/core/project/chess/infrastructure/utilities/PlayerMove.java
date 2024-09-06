package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Piece;
import jakarta.annotation.Nullable;

public record PlayerMove(Coordinate from, Coordinate to, @Nullable Piece promotion) {
}
