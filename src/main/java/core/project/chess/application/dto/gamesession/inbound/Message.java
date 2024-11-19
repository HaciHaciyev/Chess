package core.project.chess.application.dto.gamesession.inbound;


public record Message(String message) {

    public Message {
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
    }
}
