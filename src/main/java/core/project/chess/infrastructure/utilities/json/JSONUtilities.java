package core.project.chess.infrastructure.utilities.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.*;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    public static Result<Message, Throwable> readAsMessage(String message) {
        Log.infof("deserializing: %s", message);
        try {
            return Result.success(objectMapper.readValue(message, Message.class));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public static Result<MessageType, Throwable> chessMessageType(final String message) {
        Result<JsonNode, Throwable> resultNode = jsonTree(message);
        if (!resultNode.success()) {
            return Result.failure(resultNode.throwable());
        }

        return Result.ofThrowable(() -> MessageType.valueOf(resultNode.value().get("type").asText()));
    }

    public static Result<ChatMessage, Throwable> messageRecord(JsonNode messageNode) {
        return Result.ofThrowable(() -> new ChatMessage(messageNode.get("message").asText()));
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

    public static Result<String, Throwable> gameSessionToString(ChessGame chessGame) {
        try {
            var message = new GameSessionMessage(
                    chessGame.getChessGameId().toString(),
                    chessGame.getPlayerForWhite().getUsername(), chessGame.getPlayerForBlack().getUsername(),
                    chessGame.getPlayerForWhiteRating().rating(), chessGame.getPlayerForBlackRating().rating(),
                    chessGame.getTimeControllingTYPE()
            );

            return Result.success(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
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
        final boolean connectToExistedGame = Objects.nonNull(gameId.value());

        if (connectToExistedGame) {
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

        return Result.success(new GameInit(null, color.value(), time.value(), nameOfPartner.value()));
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

    public static Result<String, Throwable> gameId(String message) {
        final Result<JsonNode, Throwable> node = jsonTree(message);
        if (!node.success()) {
            return Result.failure(node.throwable());
        }

        return Result.ofThrowable(
                () -> node.value().has("game-id") && !node.value().get("game-id").isNull() ? node.value().get("game-id").asText() : null
        );
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
        } catch (Throwable e) {
            return Result.failure(e);
        }
    }
}
