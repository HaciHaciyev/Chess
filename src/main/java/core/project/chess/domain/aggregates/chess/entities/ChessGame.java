package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.value_objects.Coordinate;
import core.project.chess.domain.aggregates.chess.value_objects.Piece;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.infrastructure.utilities.StatusPair;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

@Getter
public class ChessGame {
    private final UUID chessGameId;
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating playerForWhiteRating;
    private final Rating playerForBlackRating;
    private final SessionEvents sessionEvents;
    private final TimeControllingTYPE timeControllingTYPE;
    private @Getter(AccessLevel.NONE) StatusPair<Operations> isGameOver;

    private ChessGame(
            UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite, UserAccount playerForBlack, Rating playerForWhiteRating,
            Rating playerForBlackRating, SessionEvents sessionEvents, TimeControllingTYPE timeControllingTYPE,
            StatusPair<Operations> statusPair
    ) {
        Objects.requireNonNull(chessGameId);
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(playerForWhite);
        Objects.requireNonNull(playerForBlack);
        Objects.requireNonNull(playerForWhiteRating);
        Objects.requireNonNull(playerForBlackRating);
        Objects.requireNonNull(sessionEvents);
        Objects.requireNonNull(timeControllingTYPE);
        Objects.requireNonNull(statusPair);

        this.chessGameId = chessGameId;
        this.chessBoard = chessBoard;
        this.playerForWhite = playerForWhite;
        this.playerForBlack = playerForBlack;
        this.playerForWhiteRating = playerForWhiteRating;
        this.playerForBlackRating = playerForBlackRating;
        this.sessionEvents = sessionEvents;
        this.timeControllingTYPE = timeControllingTYPE;
        this.isGameOver = statusPair;
    }

    public static ChessGame of(
            UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite, UserAccount playerForBlack,
            SessionEvents sessionEvents, TimeControllingTYPE timeControllingTYPE
    ) {
        return new ChessGame(
                chessGameId, chessBoard, playerForWhite, playerForBlack, playerForWhite.getRating(),
                playerForBlack.getRating(), sessionEvents, timeControllingTYPE, StatusPair.ofFalse()
        );
    }

    public static ChessGame fromRepository(
            UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite, UserAccount playerForBlack,
            Rating whitePlayerRating, Rating blackPlayerRating, SessionEvents sessionEvents,
            TimeControllingTYPE timeControllingTYPE, StatusPair<Operations> statusPair
    ) {
        return new ChessGame(
                chessGameId, chessBoard, playerForWhite, playerForBlack, whitePlayerRating, blackPlayerRating,
                sessionEvents, timeControllingTYPE, statusPair
        );
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

    public void returnMovement() {
        final boolean successfulMoveReturning = chessBoard.returnOfTheMovement();
        if (!successfulMoveReturning) {
            throw new IllegalArgumentException("Can`t return the move.");
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
                Objects.equals(playerForWhiteRating, chessGame.playerForWhiteRating) &&
                Objects.equals(playerForBlackRating, chessGame.playerForBlackRating) &&
                Objects.equals(sessionEvents, chessGame.sessionEvents) &&
                timeControllingTYPE == chessGame.timeControllingTYPE &&
                Objects.equals(isGameOver, chessGame.isGameOver);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chessBoard);
        result = 31 * result + Objects.hashCode(playerForWhite);
        result = 31 * result + Objects.hashCode(playerForBlack);
        result = 31 * result + Objects.hashCode(playerForWhiteRating);
        result = 31 * result + Objects.hashCode(playerForBlackRating);
        result = 31 * result + Objects.hashCode(sessionEvents);
        result = 31 * result + Objects.hashCode(timeControllingTYPE);
        result = 31 * result + Objects.hashCode(isGameOver);
        return result;
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
