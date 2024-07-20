package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.GameEvents;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GameOfChess {
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating whitePlayerRating;
    private final Rating blackPlayerRating;
    private final GameEvents gameEvents;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GameOfChess that = (GameOfChess) o;
        return Objects.equals(chessBoard, that.chessBoard) &&
                Objects.equals(playerForWhite.getId(), that.playerForBlack.getId()) &&
                Objects.equals(whitePlayerRating, that.whitePlayerRating) &&
                Objects.equals(blackPlayerRating, that.blackPlayerRating) &&
                Objects.equals(gameEvents, that.gameEvents);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chessBoard);
        result = 31 * result + Objects.hashCode(playerForWhite.getId());
        result = 31 * result + Objects.hashCode(playerForBlack.getId());
        result = 31 * result + Objects.hashCode(whitePlayerRating);
        result = 31 * result + Objects.hashCode(blackPlayerRating);
        result = 31 * result + Objects.hashCode(gameEvents);
        return result;
    }

    public static class Builder {
        private ChessBoard chessBoard;
        private UserAccount playerForWhite;
        private UserAccount playerForBlack;
        private Rating whitePlayerRating;
        private Rating blackPlayerRating;
        private GameEvents gameEvents;

        private Builder() {}

        public Builder chessBoard(ChessBoard chessBoard) {
            this.chessBoard = chessBoard;
            return this;
        }

        public Builder playerForWhite(UserAccount playerForWhite) {
            this.playerForWhite = playerForWhite;
            return this;
        }

        public Builder playerForBlack(UserAccount playerForBlack) {
            this.playerForBlack = playerForBlack;
            return this;
        }

        public Builder whitePlayerRating(Rating whitePlayerRating) {
            this.whitePlayerRating = whitePlayerRating;
            return this;
        }

        public Builder blackPlayerRating(Rating blackPlayerRating) {
            this.blackPlayerRating = blackPlayerRating;
            return this;
        }

        public Builder gameEvents(GameEvents gameEvents) {
            this.gameEvents = gameEvents;
            return this;
        }

        public GameOfChess build() {
            Objects.requireNonNull(playerForWhite);
            Objects.requireNonNull(playerForBlack);

            return new GameOfChess(
                    Objects.requireNonNullElse(chessBoard, ChessBoard.initialPosition()), playerForWhite, playerForBlack,
                    Objects.requireNonNullElse(whitePlayerRating, playerForWhite.getRating()),
                    Objects.requireNonNullElse(blackPlayerRating, playerForBlack.getRating()),
                    Objects.requireNonNullElse(gameEvents, GameEvents.defaultEvents())
            );
        }
    }
}
