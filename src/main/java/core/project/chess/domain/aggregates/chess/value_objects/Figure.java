package core.project.chess.domain.aggregates.chess.value_objects;

public sealed interface Figure
        permits Knight, Bishop, Rook, Queen, King {

    boolean isValidMove(Coordinate start, Coordinate end);
}

