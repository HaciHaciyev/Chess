package core.project.chess.application.controller;

import core.project.chess.application.dto.gamesession.Message;
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

import java.util.Objects;
import java.util.Optional;

import static core.project.chess.infrastructure.utilities.web.WSUtilities.sendMessage;

@ServerEndpoint("/chessland/chess-game")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameHandler {

    private final JwtUtility jwtUtility;

    private final ChessGameService chessGameService;

    @OnOpen
    public void onOpen(final Session session) {
        final Optional<JsonWebToken> jwt = jwtUtility.extractJWT(session);
        if (jwt.isEmpty()) {
            sendMessage(session, Message.error("Token is required.").asJSON());
            WSUtilities.closeSession(session, Message.error("You are not authorized.").asJSON());
            return;
        }

        final Username username = new Username(jwt.orElseThrow().getName());
        chessGameService.onOpen(session, username);
    }

    @OnMessage
    public void onMessage(final Session session, final String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, Message.error("Message is required.").asJSON());
            return;
        }

        if (message.length() > 512) {
            sendMessage(session, Message.error("Message is to long.").asJSON());
        }

        final Optional<JsonWebToken> jwt = jwtUtility.extractJWT(session);
        if (jwt.isEmpty()) {
            sendMessage(session, Message.error("Token is required.").asJSON());
            WSUtilities.closeSession(session, Message.error("You are not authorized.").asJSON());
            return;
        }

        final Username username = new Username(jwt.orElseThrow().getName());
        chessGameService.onMessage(session, username, message);
    }

    @OnClose
    public void onClose(final Session session) {
        chessGameService.onClose(session);
    }
}
