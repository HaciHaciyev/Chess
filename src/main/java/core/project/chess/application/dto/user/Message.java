package core.project.chess.application.dto.user;

import java.util.Objects;

public record Message(String message) {

    public Message {
        Objects.requireNonNull(message);
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message can`t be blank.");
        }
    }
}
