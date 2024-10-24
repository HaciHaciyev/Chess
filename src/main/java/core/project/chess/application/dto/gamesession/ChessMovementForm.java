package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.Piece;
import core.project.chess.infrastructure.utilities.annotations.OptionalArgument;

public record ChessMovementForm(Coordinate from, Coordinate to, @OptionalArgument Piece inCaseOfPromotion) { }
