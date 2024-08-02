package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.chess.value_objects.Piece;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.infrastructure.utilities.StatusPair;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessGame {
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating whitePlayerRating;
    private final Rating blackPlayerRating;
    private final SessionEvents sessionEvents;
    private final TimeControllingTYPE timeControllingTYPE;
    private @Getter(AccessLevel.NONE) StatusPair<Operations> isGameOver;

    public static Builder builder() {
        return new Builder();
    }

    private void gameOver(Operations operation) {
        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game was over.");
        }
        isGameOver = StatusPair.ofTrue(operation);
    }

    public void makeMovement(
            final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion
    ) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        Operations operation = chessBoard.reposition(from, to, inCaseOfPromotion);

        final boolean gameOver = operation.equals(Operations.STALEMATE) || operation.equals(Operations.CHECKMATE);
        if (gameOver) {
            gameOver(operation);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(chessBoard, chessGame.chessBoard) &&
                Objects.equals(playerForWhite, chessGame.playerForWhite) &&
                Objects.equals(playerForBlack, chessGame.playerForBlack) &&
                Objects.equals(whitePlayerRating, chessGame.whitePlayerRating) &&
                Objects.equals(blackPlayerRating, chessGame.blackPlayerRating) &&
                Objects.equals(sessionEvents, chessGame.sessionEvents) &&
                timeControllingTYPE == chessGame.timeControllingTYPE &&
                Objects.equals(isGameOver, chessGame.isGameOver);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chessBoard);
        result = 31 * result + Objects.hashCode(playerForWhite);
        result = 31 * result + Objects.hashCode(playerForBlack);
        result = 31 * result + Objects.hashCode(whitePlayerRating);
        result = 31 * result + Objects.hashCode(blackPlayerRating);
        result = 31 * result + Objects.hashCode(sessionEvents);
        result = 31 * result + Objects.hashCode(timeControllingTYPE);
        result = 31 * result + Objects.hashCode(isGameOver);
        return result;
    }

    public static class Builder {
        private ChessBoard chessBoard;
        private UserAccount playerForWhite;
        private UserAccount playerForBlack;
        private Rating whitePlayerRating;
        private Rating blackPlayerRating;
        private SessionEvents sessionEvents;
        private TimeControllingTYPE timeControllingTYPE;

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

        public Builder setWhitePlayerRating(Rating whitePlayerRating) {
            this.whitePlayerRating = whitePlayerRating;
            return this;
        }

        public Builder setBlackPlayerRating(Rating blackPlayerRating) {
            this.blackPlayerRating = blackPlayerRating;
            return this;
        }

        public Builder setSessionEvents(SessionEvents sessionEvents) {
            this.sessionEvents = sessionEvents;
            return this;
        }

        public Builder timeControllingTYPE(TimeControllingTYPE timeControllingTYPE) {
            this.timeControllingTYPE = timeControllingTYPE;
            return this;
        }

        public ChessGame build() {
            Objects.requireNonNull(chessBoard);
            Objects.requireNonNull(playerForWhite);
            Objects.requireNonNull(playerForBlack);
            Objects.requireNonNull(timeControllingTYPE);

            return new ChessGame(
                    chessBoard, playerForWhite, playerForBlack,
                    whitePlayerRating, blackPlayerRating,
                    sessionEvents, timeControllingTYPE, StatusPair.ofFalse()
            );
        }
    }

    @Getter
    public enum TimeControllingTYPE {
        BULLET(1),
        BLITZ(5),
        RAPID(10),
        CLASSIC(30),
        DEFAULT(180);

        private final int minutes;

        TimeControllingTYPE(int minutes) {
            this.minutes = minutes;
        }
    }
}
