package core.project.chess.application.dto.gamesession.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.user.value_objects.Username;

public record GameSessionMessage(String id,
                                 Username whitePlayerUsername, Username blackPlayerUsername,
                                 double whitePlayerRating, double blackPlayerRating,
                                 ChessGame.TimeControllingTYPE timeControl) {
}
