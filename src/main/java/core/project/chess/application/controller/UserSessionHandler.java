package core.project.chess.application.controller;

import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.services.GlobalSessionService;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.utilities.web.WSUtilities;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@ServerEndpoint("/user-session")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserSessionHandler {

    private final JwtUtility jwtUtility;

    private final GlobalSessionService globalSessionService;

    @OnOpen
    public final void onOpen(Session session) {
        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        globalSessionService.handleOnOpen(session, username);
    }

    @OnMessage
    public final void onMessage(Session session, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            WSUtilities.sendMessage(session, "Message is required.");
            return;
        }

        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        globalSessionService.handleOnMessage(session, username, message);
    }

    @OnClose
    public final void onClose(Session session) {
        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        globalSessionService.handleOnClose(session, username);
    }
}
