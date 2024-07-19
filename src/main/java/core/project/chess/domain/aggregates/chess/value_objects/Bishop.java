package core.project.chess.domain.aggregates.chess.value_objects;

public record Bishop(Color color)
        implements Figure {

    @Override
    public boolean isValidMove(Coordinate start, Coordinate end) {
        return false;
    }
}
