package core.project.chess.application.util;

import core.project.chess.application.dto.chess.Message;
import io.quarkus.logging.Log;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

public class WSUtilities {

    private WSUtilities() {}

    /**
     * Closes session with specified message as reason (can be null)
     */
    public static void closeSession(final Session currentSession, final String message) {
        try {
            currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, message));
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
    }

    public static void closeSession(final Session currentSession, final Message message) {
        closeSession(currentSession, message.asJSON());
    }

    /**
     * Sends message to specified session
     */
    public static void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
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
