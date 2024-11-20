package core.project.chess.application.dto.gamesession;


import java.util.Objects;

public record ChatMessage(String message) {

    public ChatMessage {
        Objects.requireNonNull(message);

        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
    }
}
