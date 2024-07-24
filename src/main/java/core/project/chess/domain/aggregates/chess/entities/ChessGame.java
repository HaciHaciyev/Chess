package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.Color;
import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.Operations;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessGame {
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final boolean isTimeControlEnable;
    private final TimeControllingTYPE timeControllingTYPE;
    private Color currentPlayer;
    private boolean isGameOver;

    public static Builder builder() {
        return new Builder();
    }

    private void gameOver() {
        isGameOver = true;
    }

    public void makeMovement(final Coordinate from, final Coordinate to) throws IllegalAccessException {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        validatePlayerTurn(from);
        Operations operation = chessBoard.reposition(from, to);

        final boolean gameOver = operation.equals(Operations.STALEMATE) || operation.equals(Operations.CHECKMATE);
        if (gameOver) {
            gameOver();
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
    private void validatePlayerTurn(final Coordinate coordinate) throws IllegalAccessException {
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
                        chessBoard, playerForWhite, playerForBlack, true, timeControllingTYPE, Color.WHITE, false
                );
            } else {
                return new ChessGame(
                        chessBoard, playerForWhite, playerForBlack, false, null, Color.WHITE, false
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
