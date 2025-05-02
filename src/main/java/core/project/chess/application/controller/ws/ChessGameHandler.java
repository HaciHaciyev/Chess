package core.project.chess.application.controller.ws;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.application.service.WSAuthService;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.ws.MessageDecoder;
import core.project.chess.infrastructure.ws.MessageEncoder;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import static core.project.chess.application.util.WSUtilities.closeSession;

@ServerEndpoint(value = "/chessland/chess-game", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class ChessGameHandler {

    private final WSAuthService authService;

    private final ChessGameService chessGameService;

    ChessGameHandler(WSAuthService authService, ChessGameService chessGameService) {
        this.authService = authService;
        this.chessGameService = chessGameService;
    }

    @OnOpen
    public void onOpen(final Session session) {
        Thread.startVirtualThread(() ->
                authService.validateToken(session)
                        .handle(token -> chessGameService.onOpen(session, new Username(token.getName())),
                                throwable -> closeSession(session, Message.error(throwable.getLocalizedMessage())))
        );
    }

    @OnMessage
    public void onMessage(final Session session, final Message message) {
        Thread.startVirtualThread(() ->
                authService.validateToken(session)
                        .handle(token -> chessGameService.onMessage(session, new Username(token.getName()), message),
                                throwable -> closeSession(session, Message.error(throwable.getLocalizedMessage())))
        );
    }

    @OnClose
    public void onClose(final Session session) {
        Thread.startVirtualThread(() ->
                authService.validateToken(session)
                        .handle(token -> chessGameService.onClose(session, new Username(token.getName())),
                                throwable -> closeSession(session, Message.error(throwable.getLocalizedMessage())))
        );
    }
}
