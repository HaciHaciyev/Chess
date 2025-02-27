package core.project.chess.application.dto.chess;

import core.project.chess.domain.user.value_objects.Rating;

import java.util.UUID;

public record Puzzle(UUID puzzleId, String PGN, int startPosition, Rating rating) { }
