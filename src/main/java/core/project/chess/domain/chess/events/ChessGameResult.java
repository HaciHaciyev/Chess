package core.project.chess.domain.chess.events;

import core.project.chess.domain.commons.interfaces.ChesslandDomainEvent;
import core.project.chess.domain.commons.value_objects.GameResult;
import core.project.chess.domain.commons.value_objects.RatingType;

import java.util.UUID;

public record ChessGameResult(
        UUID gameID,
        GameResult gameResult,
        UUID whitePlayer,
        UUID blackPlayer,
        RatingType ratingType) implements ChesslandDomainEvent {

    public ChessGameResult {
        if (gameID == null)
            throw new IllegalArgumentException("Game ID can`t be null");
        if (gameResult == null)
            throw new IllegalArgumentException("Game result can`t be null");
        if (whitePlayer == null)
            throw new IllegalArgumentException("White player can`t be null");
        if (blackPlayer == null)
            throw new IllegalArgumentException("Black player can`t be null");
        if (whitePlayer.equals(blackPlayer))
            throw new IllegalArgumentException("It`s can`t be the same player");
        if (gameID.equals(whitePlayer) || gameID.equals(blackPlayer))
            throw new IllegalArgumentException("Do not match");
    }
}
