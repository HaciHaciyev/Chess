package core.project.chess.application.model;

import java.util.Objects;

public record GameSearchProcessMessage(GamePreparationMessageType messageType, String message) {

    public GameSearchProcessMessage {
        Objects.requireNonNull(messageType);
        Objects.requireNonNull(message);

        if (message.isBlank()) {
            throw new IllegalArgumentException("Message can`t be blank.");
        }
    }
}
