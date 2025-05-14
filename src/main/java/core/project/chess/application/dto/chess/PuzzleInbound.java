package core.project.chess.application.dto.chess;

import core.project.chess.domain.chess.value_objects.Move;

import java.util.List;

public record PuzzleInbound(List<Move> moves, int startPositionOfPuzzle) {}
