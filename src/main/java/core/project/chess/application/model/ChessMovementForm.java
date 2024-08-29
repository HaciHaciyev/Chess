package core.project.chess.application.model;

import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.chess.value_objects.Piece;
import core.project.chess.infrastructure.utilities.OptionalArgument;

import java.util.Objects;

public record ChessMovementForm(Coordinate from, Coordinate to, @OptionalArgument Piece inCaseOfPromotion) {

    public ChessMovementForm {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }
}
