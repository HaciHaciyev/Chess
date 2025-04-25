package testUtils;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.infrastructure.ws.MessageDecoder;
import core.project.chess.infrastructure.ws.MessageEncoder;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import jakarta.websocket.*;

@ClientEndpoint(decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class WSClient {

    @Inject
    JWTParser jwtParser;

    @OnOpen
    public void onOpen(Session session) {
        String username = extractToken(session);
        Log.infof("User %s connected to the server -> %s", username, session.getRequestURI().getPath());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String username = extractToken(session);
        Log.infof("%s's session closed. Reason -> %s", username, closeReason.getReasonPhrase());
    }

    private String extractToken(Session session) {
        String query = session.getQueryString();
        String token = query.substring(query.indexOf("=") + 1);

        try {
            return jwtParser.parse(token).getName();
        } catch (ParseException e) {
            Log.info(e);
        }

        return "";
    }

    public static void sendMessage(Session session, String username, Message message) {
        if (message.type() != MessageType.MOVE)
            Log.infof("%s sending to %s -> %s", username, session.getRequestURI().toString(), message);
        session.getAsyncRemote().sendObject(message);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Log.errorf("Error: %s.", e);
        }
    }

    public static void sendMessage(final Session session, final Message message) {
        try {
            session.getAsyncRemote().sendObject(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}
