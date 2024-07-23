package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Session {
    private final ChessGame chessGame;
    private final Rating whitePlayerRating;
    private final Rating blackPlayerRating;
    private final SessionEvents sessionEvents;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Session that = (Session) o;
        return Objects.equals(chessGame, that.chessGame) &&
                Objects.equals(whitePlayerRating, that.whitePlayerRating) &&
                Objects.equals(blackPlayerRating, that.blackPlayerRating) &&
                Objects.equals(sessionEvents, that.sessionEvents);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chessGame);
        result = 31 * result + Objects.hashCode(whitePlayerRating);
        result = 31 * result + Objects.hashCode(blackPlayerRating);
        result = 31 * result + Objects.hashCode(sessionEvents);
        return result;
    }

    public static class Builder {
        private ChessGame chessGame;
        private UserAccount playerForWhite;
        private UserAccount playerForBlack;
        private Rating whitePlayerRating;
        private Rating blackPlayerRating;
        private SessionEvents sessionEvents;

        private Builder() {}

        public Builder chessBoard(ChessGame chessGame) {
            this.chessGame = chessGame;
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

        public Builder gameEvents(SessionEvents sessionEvents) {
            this.sessionEvents = sessionEvents;
            return this;
        }

        public Session build() {
            Objects.requireNonNull(playerForWhite);
            Objects.requireNonNull(playerForBlack);

            return new Session(
                    chessGame, Objects.requireNonNullElse(whitePlayerRating, playerForWhite.getRating()),
                    Objects.requireNonNullElse(blackPlayerRating, playerForBlack.getRating()),
                    Objects.requireNonNullElse(sessionEvents, SessionEvents.defaultEvents())
            );
        }
    }
}
