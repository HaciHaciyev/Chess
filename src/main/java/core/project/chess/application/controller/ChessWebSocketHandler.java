package core.project.chess.application.controller;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ChessWebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {

    }
}
