package core.project.chess.application.controller;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, ChessBoard> chessBoardMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        chessBoardMap.put(session.getId(), ChessBoard.starndardChessBoard(UUID.randomUUID()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChessBoard chessBoard = chessBoardMap.get(session.getId());

        if (chessBoard == null) {
            chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());
            chessBoardMap.put(session.getId(), chessBoard);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chessBoardMap.remove(session.getId());
    }
}
