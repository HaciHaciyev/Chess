package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;

import java.util.Objects;
import java.util.regex.Pattern;

public record Message(MessageType type,
                      String gameID,
                      String color,
                      String partner,
                      Coordinate from,
                      Coordinate to,
                      String inCaseOfPromotion,
                      String message,
                      TimeControllingTYPE time) {

    private static final String INVITATION_MESSAGE = "User %s invite you for a chess game with parameters: figures color for you = %s, time control = %s.";
    private static final Pattern pattern = Pattern.compile("^[QRNBqrnb]$");

    public Message {
        Objects.requireNonNull(type);

        if (Objects.nonNull(inCaseOfPromotion) && !pattern.matcher(gameID).matches()) {
            throw new IllegalArgumentException("Invalid piece type.");
        }
    }

    public static Message gameInit(String color, TimeControllingTYPE timeControlling) {
        return new Message(MessageType.GAME_INIT, null, color, null, null, null, null, null, timeControlling);
    }

    public static Message connectToExistingGame(String gameID, String color, TimeControllingTYPE timeControlling) {
        return new Message(MessageType.GAME_INIT, gameID, color, null, null, null, null, null, timeControlling);
    }

    public static Message partnershipGame(String color, String partner, TimeControllingTYPE timeControlling) {
        return new Message(MessageType.GAME_INIT, null, color, partner, null, null, null, null, timeControlling);
    }

    public static Message move(String gameID, Coordinate from, Coordinate to) {
        return new Message(MessageType.MOVE, gameID, null, null, from, to, null, null, null);
    }

    public static Message promotion(String gameID, Coordinate from, Coordinate to, String promotion) {
        return new Message(MessageType.MOVE, gameID, null, null, from, to, promotion, null,  null);
    }

    public static Message chat(String gameID, String message) {
        return new Message(MessageType.MESSAGE, gameID, null, null, null, null, null, message, null);
    }

    public static Message returnMovement(String gameID) {
        return new Message(MessageType.MOVE, gameID, null, null, null, null, null, null, null);
    }

    public static Message resignation(String gameID) {
        return new Message(MessageType.RESIGNATION, gameID, null, null, null, null, null, null, null);
    }

    public static Message threefold(String gameID) {
        return new Message(MessageType.TREE_FOLD, gameID, null, null, null, null, null, null, null);
    }

    public static Message agreement(String gameID) {
        return new Message(MessageType.AGREEMENT, gameID, null, null, null, null, null, null, null);
    }

    public static Message error(String message) {
        return new Message(MessageType.ERROR, null, null, null, null, null, null, message, null);
    }

    public static Message invitation(String username, GameParameters gameParams) {
        String message = String.format(INVITATION_MESSAGE, username, gameParams.color(), gameParams.timeControllingTYPE());
        return new Message(MessageType.INVITATION, null, null, null, null, null, null, message, null);
    }

    public static Message info(String message) {
        return new Message(MessageType.INFO, null, null, null, null, null, null, message, null);
    }
}
