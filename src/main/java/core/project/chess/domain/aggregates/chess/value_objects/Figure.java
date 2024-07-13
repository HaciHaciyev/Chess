package core.project.chess.domain.aggregates.chess.value_objects;

public sealed interface Figure permits Pawn, Knight, Bishop, Rook, Queen, King {

}
