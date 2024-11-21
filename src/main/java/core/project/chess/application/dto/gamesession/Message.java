package core.project.chess.application.dto.gamesession;

import core.project.chess.domain.aggregates.chess.entities.ChessGame.TimeControllingTYPE;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.json.JSONUtilities;

import java.util.Objects;
import java.util.regex.Pattern;

public record Message(MessageType type,
                      String gameID,
                      String FEN,
                      String PGN,
                      Username whitePlayerUsername,
                      Username blackPlayerUsername,
                      Double whitePlayerRating,
                      Double blackPlayerRating,
                      String color,
                      String partner,
                      Coordinate from,
                      Coordinate to,
                      String inCaseOfPromotion,
                      String message,
                      TimeControllingTYPE time) {

    private static final Pattern PROMOTION_PATTERN = Pattern.compile("^[QRNBqrnb]$");
    private static final String INVITATION_MESSAGE = "User %s invite you for a chess game with parameters: figures color for you = %s, time control = %s.";

    public Message {
        Objects.requireNonNull(type, "Message type must not be null.");
        if (Objects.nonNull(inCaseOfPromotion) && !PROMOTION_PATTERN.matcher(inCaseOfPromotion).matches()) {
            throw new IllegalArgumentException("Invalid promotion piece type.");
        }
    }

    public static Builder builder(MessageType type) {
        return new Builder(type);
    }

    public static String gameInit(String color, TimeControllingTYPE time) {
        return builder(MessageType.GAME_INIT)
                .color(color)
                .time(time)
                .build()
                .asJSON();
    }

    public static String move(String gameID, Coordinate from, Coordinate to) {
        return builder(MessageType.MOVE)
                .gameID(gameID)
                .from(from)
                .to(to)
                .build()
                .asJSON();
    }

    public static String promotion(String gameID, Coordinate from, Coordinate to, String promotion) {
        return builder(MessageType.MOVE)
                .gameID(gameID)
                .from(from)
                .to(to)
                .inCaseOfPromotion(promotion)
                .build()
                .asJSON();
    }

    public static String chat(String gameID, String message) {
        return builder(MessageType.MESSAGE)
                .gameID(gameID)
                .message(message)
                .build()
                .asJSON();
    }

    public static String invitation(String username, GameParameters gameParams) {
        String message = String.format(INVITATION_MESSAGE, username, gameParams.color(), gameParams.timeControllingTYPE());
        return builder(MessageType.INVITATION)
                .message(message)
                .build()
                .asJSON();
    }

    public static String connectToExistingGame(String gameID, String color, TimeControllingTYPE time) {
        return builder(MessageType.GAME_INIT)
                .gameID(gameID)
                .color(color)
                .time(time)
                .build()
                .asJSON();
    }

    public static String partnershipGame(String color, String partner, TimeControllingTYPE time) {
        return builder(MessageType.GAME_INIT)
                .color(color)
                .partner(partner)
                .time(time)
                .build()
                .asJSON();
    }

    public static String returnMovement(String gameID) {
        return builder(MessageType.MOVE)
                .gameID(gameID)
                .build()
                .asJSON();
    }

    public static String resignation(String gameID) {
        return builder(MessageType.RESIGNATION)
                .gameID(gameID)
                .build()
                .asJSON();
    }

    public static String threefold(String gameID) {
        return builder(MessageType.TREE_FOLD)
                .gameID(gameID)
                .build()
                .asJSON();
    }

    public static String agreement(String gameID) {
        return builder(MessageType.AGREEMENT)
                .gameID(gameID)
                .build()
                .asJSON();
    }

    public static String error(String message) {
        return builder(MessageType.ERROR)
                .message(message)
                .build()
                .asJSON();
    }

    public static String info(String message) {
        return builder(MessageType.INFO)
                .message(message)
                .build()
                .asJSON();
    }

    public static String userInfo(String message) {
        return builder(MessageType.USER_INFO)
                .message(message)
                .build()
                .asJSON();
    }

    public static String gameStartInfo(GameSessionMessage gsm) {
        return builder(MessageType.GAME_START_INFO)
                .gameID(gsm.id())
                .whitePlayerUsername(gsm.whitePlayerUsername())
                .blackPlayerUsername(gsm.blackPlayerUsername())
                .whitePlayerRating(gsm.whitePlayerRating())
                .blackPlayerRating(gsm.blackPlayerRating())
                .time(gsm.timeControl())
                .build()
                .asJSON();
    }

    public static String FEN_PGN(String gameID, String FEN, String PGN) {
        return builder(MessageType.FEN_PGN)
                .gameID(gameID)
                .FEN(FEN)
                .PGN(PGN)
                .build()
                .asJSON();
    }

    public String asJSON() {
        return JSONUtilities.writeMessage(this);
    }

    public static class Builder {
        private final MessageType type;
        private String gameID;
        private String FEN;
        private String PGN;
        private Username whitePlayerUsername;
        private Username blackPlayerUsername;
        private Double whitePlayerRating;
        private Double blackPlayerRating;
        private String color;
        private String partner;
        private Coordinate from;
        private Coordinate to;
        private String inCaseOfPromotion;
        private String message;
        private TimeControllingTYPE time;

        public Builder(MessageType type) {
            this.type = Objects.requireNonNull(type, "Message type must not be null.");
        }

        public Builder gameID(String gameID) {
            this.gameID = gameID;
            return this;
        }

        public Builder FEN(String FEN) {
            this.FEN = FEN;
            return this;
        }

        public Builder PGN(String PGN) {
            this.PGN = PGN;
            return this;
        }

        public Builder whitePlayerUsername(Username whitePlayerUsername) {
            this.whitePlayerUsername = whitePlayerUsername;
            return this;
        }

        public Builder blackPlayerUsername(Username blackPlayerUsername) {
            this.blackPlayerUsername = blackPlayerUsername;
            return this;
        }

        public Builder whitePlayerRating(Double whitePlayerRating) {
            this.whitePlayerRating = whitePlayerRating;
            return this;
        }

        public Builder blackPlayerRating(Double blackPlayerRating) {
            this.blackPlayerRating = blackPlayerRating;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder partner(String partner) {
            this.partner = partner;
            return this;
        }

        public Builder from(Coordinate from) {
            this.from = from;
            return this;
        }

        public Builder to(Coordinate to) {
            this.to = to;
            return this;
        }

        public Builder inCaseOfPromotion(String inCaseOfPromotion) {
            this.inCaseOfPromotion = inCaseOfPromotion;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder time(TimeControllingTYPE time) {
            this.time = time;
            return this;
        }

        public Message build() {
            return new Message(type, gameID, FEN, PGN, whitePlayerUsername, blackPlayerUsername,
                    whitePlayerRating, blackPlayerRating, color, partner,
                    from, to, inCaseOfPromotion, message, time);
        }
    }
}