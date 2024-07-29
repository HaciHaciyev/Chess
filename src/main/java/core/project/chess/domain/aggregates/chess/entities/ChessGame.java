package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.value_objects.Color;
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

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessGame {
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating whitePlayerRating;
    private final Rating blackPlayerRating;
    private final SessionEvents sessionEvents;
    private final boolean isTimeControlEnable;
    private final TimeControllingTYPE timeControllingTYPE;
    private Color currentPlayer;
    private StatusPair<Operations> isGameOver;

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
    ) throws IllegalAccessException {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        validateFiguresTurn(from);
        Operations operation = chessBoard.reposition(from, to, inCaseOfPromotion);

        final boolean gameOver = operation.equals(Operations.STALEMATE) || operation.equals(Operations.CHECKMATE);
        if (gameOver) {
            gameOver(operation);
        }

        switchPlayer();
    }

    private void switchPlayer() {
        if (currentPlayer.equals(Color.WHITE)) {
            currentPlayer = Color.BLACK;
        } else {
            currentPlayer = Color.WHITE;
        }
    }

    /** This is not a complete validation of which player should play at this point.
     * This validation rather checks what color pieces should be moved.
     * Finally, validation of the question of who should walk can only be carried out in the controller.*/
    private void validateFiguresTurn(final Coordinate coordinate) throws IllegalAccessException {
        Color activePlayer = chessBoard.pieceColor(coordinate).orElseThrow();
        if (currentPlayer != activePlayer) {
            throw new IllegalAccessException(
                    String.format("At the moment, the player for %s must move and not the player for %s", currentPlayer, activePlayer)
            );
        }
    }

    public static class Builder {
        private ChessBoard chessBoard;
        private UserAccount playerForWhite;
        private UserAccount playerForBlack;
        private Rating whitePlayerRating;
        private Rating blackPlayerRating;
        private SessionEvents sessionEvents;
        private boolean isTimeControlEnable;
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

        public Builder timeControlDisable() {
            isTimeControlEnable = false;
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

            if (!isTimeControlEnable && timeControllingTYPE != null) {
                throw new IllegalStateException("You could`t set time controlling if it`s disable");
            }

            if (isTimeControlEnable) {
                return new ChessGame(
                        chessBoard, playerForWhite, playerForBlack, whitePlayerRating, blackPlayerRating,
                        sessionEvents, true, timeControllingTYPE, Color.WHITE, StatusPair.ofFalse()
                );
            } else {
                return new ChessGame(
                        chessBoard, playerForWhite, playerForBlack, whitePlayerRating, blackPlayerRating,
                        sessionEvents, false, null, Color.WHITE, StatusPair.ofFalse()
                );
            }
        }
    }

    @Getter
    public enum TimeControllingTYPE {
        BULLET(1),
        BLITZ(5),
        RAPID(10),
        CLASSIC(30);

        private final int minutes;

        TimeControllingTYPE(int minutes) {
            this.minutes = minutes;
        }
    }
}
