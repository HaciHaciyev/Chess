package core.project.chess.application.model;

import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.chess.value_objects.Piece;
import jakarta.annotation.Nullable;

public record ChessMovementForm(
        Coordinate from, Coordinate to, @Nullable Piece inCaseOfPromotion
) {

}
