package core.project.chess.infrastructure.utilities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.user.MessageType;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;

public class JsonUtilities {

    private JsonUtilities() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Result<JsonNode, Throwable> jsonTree(final String message) {
        try {
            return Result.success(objectMapper.readTree(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public static Result<MessageType, Throwable> messageType(final String message) {
        Result<JsonNode, Throwable> resultNode = jsonTree(message);
        if (!resultNode.success()) {
            return Result.failure(resultNode.throwable());
        }

        return Result.ofThrowable(() -> MessageType.valueOf(resultNode.value().get("type").asText()));
    }

    public static Result<core.project.chess.application.dto.gamesession.MessageType, Throwable> chessMessageType(final String message) {
        Result<JsonNode, Throwable> resultNode = jsonTree(message);
        if (!resultNode.success()) {
            return Result.failure(resultNode.throwable());
        }

        return Result.ofThrowable(() -> core.project.chess.application.dto.gamesession.MessageType.valueOf(resultNode.value().get("type").asText()));
    }

    public static Result<String, Throwable> message(JsonNode messageNode) {
        return Result.ofThrowable(() -> messageNode.get("message").asText());
    }

    public static Result<Username, Throwable> usernameOfPartner(JsonNode messageNode) {
        return Result.ofThrowable(() -> new Username(messageNode.get("usernameOfPartner").asText()));
    }
}
