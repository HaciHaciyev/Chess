package core.project.chess.application.controller;

import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.config.security.JwtUtility;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@ServerEndpoint("/chess-game")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JwtUtility jwtUtility;

    private final ChessGameService chessGameService;

    @OnOpen
    public void onOpen(final Session session) {
        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        chessGameService.handleOnOpen(session, username);
    }

    @OnMessage
    public void onMessage(final Session session, final String message) {
        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        chessGameService.handleOnMessage(session, username, message);
    }

    @OnClose
    public void onClose(final Session session) {
        chessGameService.handleOnClose(session);
    }
}
