package core.project.chess.domain.services;

import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.websocket.Session;

public interface GlobalSessionService {

    void handleOnOpen(Session session, Username username);

    void handleOnMessage(Session session, Username username, String message);

    void handleOnClose(Session session, Username username);
}
