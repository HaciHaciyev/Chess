package core.project.chess.domain.chess.value_objects;

import java.util.UUID;

public record PuzzleStateUpdate(
        UUID gameID,
        String fen,
        String pgn,
        boolean isPuzzleEnded,
        boolean isPuzzleSolved
) {}
