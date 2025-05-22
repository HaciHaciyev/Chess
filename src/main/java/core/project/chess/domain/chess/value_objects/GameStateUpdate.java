package core.project.chess.domain.chess.value_objects;

import java.util.UUID;

public record GameStateUpdate(
        UUID gameID,
        String fen,
        String pgn,
        String remainingTime,
        boolean threeFoldActive
) {}