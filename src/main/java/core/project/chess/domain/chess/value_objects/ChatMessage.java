package core.project.chess.domain.chess.value_objects;


import java.util.Objects;

public record ChatMessage(String message) {

    public ChatMessage {
        Objects.requireNonNull(message);
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }

        if (message.length() > 255) {
            throw new IllegalArgumentException("Message cannot be longer than 255 characters.");
        }
    }
}
