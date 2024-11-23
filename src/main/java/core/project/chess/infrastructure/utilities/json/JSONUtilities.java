package core.project.chess.infrastructure.utilities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.*;
import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.time.LocalDateTime;
import java.util.Optional;

public class JSONUtilities {

    private JSONUtilities() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Result<JsonNode, Throwable> jsonTree(final String message) {
        try {
            return Result.success(objectMapper.readTree(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public static Optional<String> write(Message message) {
        try {
            return Optional.of(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public static Result<String, Throwable> writeValueAsString(Object value) {
        try {
            final String message = objectMapper.writeValueAsString(value);

            return Result.success(message);
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public static Result<GameParameters, Throwable> gameParameters(String value) {
        final Result<JsonNode, Throwable> node = jsonTree(value);
        if (!node.success()) {
            return Result.failure(node.throwable());
        }

        GameParameters gameParameters = new GameParameters(
                Color.valueOf(node.value().get("color").asText()),
                TimeControllingTYPE.valueOf(node.value().get("timeControllingTYPE").asText()),
                LocalDateTime.parse(node.value().get("creationTime").asText())
        );

        return Result.success(gameParameters);
    }
}
