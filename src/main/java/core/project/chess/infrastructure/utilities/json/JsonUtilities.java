package core.project.chess.infrastructure.utilities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.ChessGameMessage;
import core.project.chess.application.dto.gamesession.ChessMovementForm;
import core.project.chess.application.dto.gamesession.GameInit;
import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.application.dto.user.MessageType;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.util.Objects;
import java.util.UUID;

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

    public static Result<ChessMovementForm, Throwable> movementFormMessage(final JsonNode node) {
        try {
            return Result.success(
                    new ChessMovementForm(
                            Coordinate.valueOf(node.get("from").asText()),
                            Coordinate.valueOf(node.get("to").asText()),
                            node.has("inCaseOfPromotion") && !node.get("inCaseOfPromotion").isNull()
                                    ? AlgebraicNotation.fromSymbol(node.get("inCaseOfPromotion").asText()) : null
                    )
            );
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
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

    public static Result<GameInit, Throwable> gameInit(String message) {
        final Result<JsonNode, Throwable> node = jsonTree(message);
        if (!node.success()) {
            return Result.failure(node.throwable());
        }

        final Result<UUID, Throwable> gameId = getGameId(node.value());
        if (!gameId.success()) {
            return Result.failure(gameId.throwable());
        }

        if (Objects.isNull(gameId.value())) {
            return Result.success(new GameInit(gameId.value(), null, null, null));
        }

        final Result<Color, Throwable> color = getColor(node.value());
        if (!color.success()) {
            return Result.failure(color.throwable());
        }

        final Result<TimeControllingTYPE, Throwable> time = getTimeControllingTYPE(node.value());
        if (!time.success()) {
            return Result.failure(time.throwable());
        }

        final Result<Username, Throwable> nameOfPartner = getPartnerName(node.value());
        if (!nameOfPartner.success()) {
            return Result.failure(nameOfPartner.throwable());
        }

        return Result.success(new GameInit(gameId.value(), color.value(), time.value(), nameOfPartner.value()));
    }

    private static Result<UUID, Throwable> getGameId(JsonNode node) {
        return Result.ofThrowable(
                () -> node.has("gameId") && !node.get("gameId").isNull() ? UUID.fromString(node.get("gameId").asText()) : null
        );
    }

    private static Result<Username, Throwable> getPartnerName(JsonNode node) {
        return Result.ofThrowable(
                () -> node.has("partner") && !node.get("partner").isNull() ? new Username(node.get("partner").asText()) : null
        );
    }

    private static Result<Color, Throwable> getColor(JsonNode node) {
        return Result.ofThrowable(
                () -> node.has("color") && !node.get("color").isNull() ? Color.valueOf(node.get("color").asText()) : null
        );
    }

    private static Result<TimeControllingTYPE, Throwable> getTimeControllingTYPE(JsonNode node) {
        return Result.ofThrowable(
                () -> node.has("time") && !node.get("time").isNull() ? TimeControllingTYPE.valueOf(node.get("time").asText()) : TimeControllingTYPE.DEFAULT
        );
    }
}
