package core.project.chess.application.dto.chess;

import core.project.chess.domain.chess.enumerations.Coordinate;
import jakarta.annotation.Nullable;

import java.util.List;

public record PuzzleInbound(List<Move> moves, int startPositionOfPuzzle) {

    public record Move(Coordinate from, Coordinate to, @Nullable Promotion promotion) {}

    public enum Promotion { q, r, b, n }
}
