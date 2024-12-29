package core.project.chess.application.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;

import java.util.Optional;

public class JSONUtilities {

    private JSONUtilities() {}

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

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
        try {
            return Result.success(objectMapper.readValue(value, GameParameters.class));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public static String prettyWrite(Message message) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
        } catch (JsonProcessingException e) {
            Log.error(e);
        }
        return "";
    }
}
