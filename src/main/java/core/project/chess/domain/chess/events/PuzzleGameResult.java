package core.project.chess.domain.chess.events;

import core.project.chess.domain.commons.interfaces.ChesslandDomainEvent;
import core.project.chess.domain.commons.value_objects.PuzzleStatus;
import core.project.chess.domain.commons.value_objects.Rating;

import java.util.UUID;

public record PuzzleGameResult(UUID puzzleID, Rating puzzleRating,
                               PuzzleStatus gameResult, UUID playerID) implements ChesslandDomainEvent {
    public PuzzleGameResult {
        if (puzzleID == null)
            throw new IllegalArgumentException("Puzzle id can`t be null");
        if (gameResult == null)
            throw new IllegalArgumentException("Game result can`t be null");
        if (playerID == null)
            throw new IllegalArgumentException("Player ID can`t be null");
        if (puzzleRating == null)
            throw new IllegalArgumentException("Puzzle rating can`t be null");
        if (puzzleID.equals(playerID))
            throw new IllegalArgumentException("Do not match");
    }
}
