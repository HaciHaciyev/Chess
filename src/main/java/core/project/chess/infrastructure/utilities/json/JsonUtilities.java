package core.project.chess.infrastructure.utilities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.ChessGameMessage;
import core.project.chess.application.dto.gamesession.ChessMovementForm;
import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.application.dto.user.MessageType;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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

    public static Result<Message, Throwable> messageRecord(JsonNode messageNode) {
        return Result.ofThrowable(() -> new Message(messageNode.get("message").asText()));
    }

    public static Result<Username, Throwable> usernameOfPartner(JsonNode messageNode) {
        return Result.ofThrowable(() -> new Username(messageNode.get("usernameOfPartner").asText()));
    }

    public static ChessMovementForm movementFormMessage(final JsonNode node) {
        try {
            return new ChessMovementForm(
                    Coordinate.valueOf(node.get("from").asText()),
                    Coordinate.valueOf(node.get("to").asText()),
                    node.has("inCaseOfPromotion") && !node.get("inCaseOfPromotion").isNull()
                            ? AlgebraicNotation.fromSymbol(node.get("inCaseOfPromotion").asText())
                            : null
            );
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request. Invalid move format.").build());
        }
    }

    public static Result<String, Throwable> chessGameToString(ChessGame chessGame) {
        try {
            final String message = objectMapper.writeValueAsString(
                    new ChessGameMessage(chessGame.getChessBoard().actualRepresentationOfChessBoard(), chessGame.getChessBoard().pgn())
            );

            return Result.success(message);
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }
}
