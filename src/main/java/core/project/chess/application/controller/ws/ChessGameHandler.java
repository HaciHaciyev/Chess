package core.project.chess.application.controller.ws;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.ws.MessageDecoder;
import core.project.chess.infrastructure.ws.MessageEncoder;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@ServerEndpoint(value = "/chessland/chess-game", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class ChessGameHandler {

    private final ChessGameService chessGameService;

    @OnOpen
    public void onOpen(final Session session) {
        chessGameService.validateToken(session).ifPresent(token -> chessGameService.onOpen(session, new Username(token.getName())));
    }

    @OnMessage
    public void onMessage(final Session session, final Message message) {
        chessGameService.validateToken(session).ifPresent(token -> chessGameService.onMessage(session, new Username(token.getName()), message));
    }

    @OnClose
    public void onClose(final Session session) {
        chessGameService.onClose(session);
    }

}
