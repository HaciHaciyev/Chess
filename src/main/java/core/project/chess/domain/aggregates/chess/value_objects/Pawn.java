package core.project.chess.domain.aggregates.chess.value_objects;

public record Pawn(Color color) {

    public boolean isValidMove(Coordinate start, Coordinate end) {
        return false;
    }
}
