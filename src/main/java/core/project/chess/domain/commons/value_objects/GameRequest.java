package core.project.chess.domain.commons.value_objects;

import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.user.entities.User;
import jakarta.websocket.Session;

import java.util.Objects;

public record GameRequest(Session session, User user, GameParameters gameParameters) {

    public GameRequest {
        Objects.requireNonNull(session);
        Objects.requireNonNull(user);
        Objects.requireNonNull(gameParameters);
    }
}
