package core.project.chess.application.dto.gamesession;

import java.util.Objects;

public record Message(String message) {

    public Message {
        Objects.requireNonNull(message);
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
    }
}
