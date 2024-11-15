package core.project.chess.application.controller;

import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.utilities.web.WSUtilities;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

@ServerEndpoint("/chess-game")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JwtUtility jwtUtility;

    private final ChessGameService chessGameService;

    @OnOpen
    public void onOpen(final Session session) {
        final Result<JsonWebToken, IllegalArgumentException> jwt = jwtUtility.extractJWT(session);
        if (!jwt.success()) {
            WSUtilities.sendMessage(session, "Token is required.");
            WSUtilities.closeSession(session, "You are don`t authorized.");
            return;
        }

        final Username username = new Username(jwt.value().getName());
        chessGameService.handleOnOpen(session, username);
    }

    @OnMessage
    public void onMessage(final Session session, final String message) {
        final Result<JsonWebToken, IllegalArgumentException> jwt = jwtUtility.extractJWT(session);
        if (!jwt.success()) {
            WSUtilities.sendMessage(session, "Token is required.");
            WSUtilities.closeSession(session, "You are don`t authorized.");
            return;
        }

        final Username username = new Username(jwt.value().getName());
        chessGameService.handleOnMessage(session, username, message);
    }

    @OnClose
    public void onClose(final Session session) {
        chessGameService.handleOnClose(session);
    }
}
