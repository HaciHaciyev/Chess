package core.project.chess.application.dto.chess;

import com.fasterxml.jackson.annotation.JsonInclude;
import core.project.chess.application.util.JSONUtilities;
import core.project.chess.domain.chess.entities.ChessGame.Time;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.chess.value_objects.GameStateUpdate;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.value_objects.Username;
import io.quarkus.logging.Log;

import java.util.Objects;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Message(MessageType type,
                      String gameID,
                      String FEN,
                      String PGN,
                      String whitePlayerUsername,
                      String blackPlayerUsername,
                      Double whitePlayerRating,
                      Double blackPlayerRating,
                      String timeLeft,
                      Color color,
                      String partner,
                      Coordinate from,
                      Coordinate to,
                      String inCaseOfPromotion,
                      String message,
                      Time time,
                      Boolean isCasualGame,
                      Respond respond,
                      Boolean isThreeFoldActive,
                      Boolean isPuzzleSolved,
                      Boolean isPuzzleEnded) {

    private static final Pattern PROMOTION_PATTERN = Pattern.compile("^[QRNBqrnb]$");
    private static final String INVITATION_MESSAGE = """
            User %s invite you for a chess game with parameters:
                figures color for you: %s,
                time control: %s,
                is casual game: %s.
            """;

    public Message {
        Objects.requireNonNull(type, "Message type must not be null.");
        if (Objects.nonNull(inCaseOfPromotion) && !PROMOTION_PATTERN.matcher(inCaseOfPromotion).matches()) {
            throw new IllegalArgumentException("Invalid promotion piece type.");
        }
    }

    public static Builder builder(MessageType type) {
        return new Builder(type);
    }

    public static Message gameInit(String color, Time time) {
        return builder(MessageType.GAME_INIT).color(Color.valueOf(color)).time(time).build();
    }

    public static Message move(String gameID, Coordinate from, Coordinate to) {
        return builder(MessageType.MOVE).gameID(gameID).from(from).to(to).build();
    }

    public static Message promotion(String gameID, Coordinate from, Coordinate to, String promotion) {
        return builder(MessageType.MOVE)
                .gameID(gameID)
                .from(from)
                .to(to)
                .inCaseOfPromotion(promotion)
                .build();
    }

    public static Message chat(String gameID, String message) {
        return builder(MessageType.MESSAGE).gameID(gameID).message(message).build();
    }

    public static Message invitation(String username, GameParameters gameParams) {
        String message = String.format(INVITATION_MESSAGE, username, gameParams.color(), gameParams.time(), gameParams.isCasualGame());
        return builder(MessageType.INVITATION)
                .message(message)
                .partner(username)
                .color(gameParams.color())
                .time(gameParams.time())
                .isCasualGame(gameParams.isCasualGame())
                .build();
    }

    public static Message connectToExistingGame(String gameID, Color color, Time time) {
        return builder(MessageType.GAME_INIT)
                .gameID(gameID)
                .color(color)
                .time(time)
                .build();
    }

    public static Message returnMovement(String gameID) {
        return builder(MessageType.MOVE)
                .gameID(gameID)
                .build();
    }

    public static Message resignation(String gameID) {
        return builder(MessageType.RESIGNATION)
                .gameID(gameID)
                .build();
    }

    public static Message threefold(String gameID) {
        return builder(MessageType.TREE_FOLD)
                .gameID(gameID)
                .build();
    }

    public static Message agreement(String gameID) {
        return builder(MessageType.AGREEMENT)
                .gameID(gameID)
                .build();
    }

    public static Message error(String message) {
        return builder(MessageType.ERROR)
                .message(message)
                .build();
    }

    public static Message info(String message) {
        return builder(MessageType.INFO)
                .message(message)
                .build();
    }

    public static Message userInfo(String message) {
        return builder(MessageType.USER_INFO)
                .message(message)
                .build();
    }

    public static Message FEN_PGN(String gameID, String FEN, String PGN) {
        return builder(MessageType.FEN_PGN)
                .gameID(gameID)
                .FEN(FEN)
                .PGN(PGN)
                .build();
    }

    public static Message gameStateUpdate(GameStateUpdate gameStateUpdate) {
        return builder(MessageType.FEN_PGN)
                .gameID(gameStateUpdate.gameID().toString())
                .FEN(gameStateUpdate.fen())
                .PGN(gameStateUpdate.pgn())
                .timeLeft(gameStateUpdate.remainingTime())
                .isThreeFoldActive(gameStateUpdate.threeFoldActive())
                .build();
    }

    public String asJSON() {
        try {
            return JSONUtilities.write(this).orElseThrow();
        } catch (Exception e) {
            Log.error(e);
        }

        return "";
    }

    public Result<GameParameters, IllegalArgumentException> gameParameters() {
        try {
            Time time = Objects.requireNonNullElse(this.time, Time.DEFAULT);
            return Result.success(new GameParameters(this.color, time, this.FEN, this.isCasualGame, this.PGN));
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }
    }

    public Result<Username, IllegalArgumentException> partnerUsername() {
        try {
            final Username partnerUsername = new Username(this.partner);

            return Result.success(partnerUsername);
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }
    }

    @Override
    public String toString() {
        return JSONUtilities.prettyWrite(this);
    }

    public static class Builder {
        private final MessageType type;
        private String gameID;
        private String FEN;
        private String PGN;
        private String whitePlayerUsername;
        private String blackPlayerUsername;
        private Double whitePlayerRating;
        private Double blackPlayerRating;
        private String timeLeft;
        private Color color;
        private String partner;
        private Coordinate from;
        private Coordinate to;
        private String inCaseOfPromotion;
        private String message;
        private Time time;
        private Boolean isCasualGame;
        private Respond respond;
        private Boolean isThreeFoldActive;
        private Boolean isPuzzleSolved;
        private Boolean isPuzzleEnded;

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

        public Builder whitePlayerUsername(String whitePlayerUsername) {
            this.whitePlayerUsername = whitePlayerUsername;
            return this;
        }

        public Builder blackPlayerUsername(String blackPlayerUsername) {
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

        public Builder timeLeft(String timeLeft) {
            this.timeLeft = timeLeft;
            return this;
        }

        public Builder color(Color color) {
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

        public Builder time(Time time) {
            this.time = time;
            return this;
        }

        public Builder isCasualGame(Boolean isCasualGame) {
            this.isCasualGame = isCasualGame;
            return this;
        }

        public Builder respond(Respond respond) {
            this.respond = respond;
            return this;
        }

        public Builder isThreeFoldActive(Boolean threeFoldActive) {
            isThreeFoldActive = threeFoldActive;
            return this;
        }

        public Builder isPuzzleSolved(Boolean puzzleSolved) {
            isPuzzleSolved = puzzleSolved;
            return this;
        }

        public Builder isPuzzleEnded(Boolean puzzleEnded) {
            isPuzzleEnded = puzzleEnded;
            return this;
        }

        public Message build() {
            return new Message(type, gameID, FEN, PGN, whitePlayerUsername, blackPlayerUsername,
                    whitePlayerRating, blackPlayerRating, timeLeft, color, partner,
                    from, to, inCaseOfPromotion, message, time, isCasualGame, respond, isThreeFoldActive,
                    isPuzzleSolved, isPuzzleEnded);
        }
    }

    public enum Respond {
        YES, NO
    }
}