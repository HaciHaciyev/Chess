package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.pieces.Piece;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.infrastructure.utilities.OptionalArgument;
import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ChessGame {
    private final UUID chessGameId;
    private @Getter(AccessLevel.NONE) Color playersTurn;
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating playerForWhiteRating;
    private final Rating playerForBlackRating;
    private final SessionEvents sessionEvents;
    private final TimeControllingTYPE timeControllingTYPE;
    private @Getter(AccessLevel.NONE) StatusPair<GameResult> isGameOver;

    private ChessGame(
            UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite, UserAccount playerForBlack, Rating playerForWhiteRating,
            Rating playerForBlackRating, SessionEvents sessionEvents, TimeControllingTYPE timeControllingTYPE, StatusPair<GameResult> statusPair
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
        if (playerForBlack.getId().equals(playerForWhite.getId())) {
            throw new IllegalArgumentException("Game can`t be initialize with one same player.");
        }

        this.chessGameId = chessGameId;
        this.chessBoard = chessBoard;
        this.playersTurn = Color.WHITE;
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
            TimeControllingTYPE timeControllingTYPE, StatusPair<GameResult> statusPair
    ) {
        return new ChessGame(
                chessGameId, chessBoard, playerForWhite, playerForBlack, whitePlayerRating, blackPlayerRating, sessionEvents, timeControllingTYPE, statusPair
        );
    }

    public Optional<GameResult> gameResult() {
        if (!this.isGameOver.status()) {
            return Optional.empty();
        }

        if (isGameOver.orElseThrow().equals(GameResult.DRAW)) {
            return Optional.of(GameResult.DRAW);
        }

        if (isGameOver.orElseThrow().equals(GameResult.WHITE_WIN)) {
            return Optional.of(GameResult.WHITE_WIN);
        }

        return Optional.of(GameResult.BLACK_WIN);
    }

    private void switchPlayersTurn() {
        if (playersTurn.equals(Color.WHITE)) {
            playersTurn = Color.BLACK;
        } else {
            playersTurn = Color.WHITE;
        }
    }

    public void makeMovement(
            final String username, final Coordinate from, final Coordinate to, final @OptionalArgument Piece inCaseOfPromotion
    ) throws IllegalArgumentException {

        Objects.requireNonNull(username);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        final boolean thirdPartyUser = !isWhitePlayer && !isBlackPlayer;
        if (thirdPartyUser) {
            throw new IllegalArgumentException("Not a player.");
        }

        final boolean whiteTriesToMoveButNotHistTurn = isWhitePlayer && !Color.WHITE.equals(playersTurn);
        if (whiteTriesToMoveButNotHistTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }

        final boolean blackTriesToMoveButNotHistTurn = isBlackPlayer && !Color.BLACK.equals(playersTurn);
        if (blackTriesToMoveButNotHistTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }

        final Operations operation = chessBoard.reposition(from, to, inCaseOfPromotion);

        final boolean gameOver = operation.equals(Operations.STALEMATE) || operation.equals(Operations.CHECKMATE);
        if (gameOver) {
            log.info("GAME OVER: {}", operation);
            gameOver(operation);
            return;
        }

        switchPlayersTurn();
    }

    public void returnMovement(final UUID userId) throws IllegalAccessException {
        Objects.requireNonNull(userId);

        final boolean isWhitePlayer = userId.equals(playerForWhite.getId());
        final boolean isBlackPlayer = userId.equals(playerForBlack.getId());

        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalAccessException("Not a player.");
        }

        if (isGameOver.status()) {
            throw new IllegalAccessException("Game is over.");
        }

        final boolean successfulMoveReturning = chessBoard.returnOfTheMovement();
        if (!successfulMoveReturning) {
            throw new IllegalArgumentException("Can`t return the move.");
        }

        switchPlayersTurn();
    }

    private void gameOver(final Operations operation) {
        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game was over.");
        }

        if (operation.equals(Operations.STALEMATE)) {
            log.info("STALEMATE");
            drawEnding();
            return;
        }

        if (operation.equals(Operations.CHECKMATE)) {
            log.info("CHECKMATE");
            winnerEnding();
            return;
        }

        throw new IllegalStateException("Invalid method usage, check documentation.");
    }

    private void drawEnding() {
        this.isGameOver = StatusPair.ofTrue(GameResult.DRAW);
        calculatePlayersRating();
    }

    private void winnerEnding() {
        GameResult gameResult;
        if (playersTurn.equals(Color.WHITE)) {
            gameResult = GameResult.WHITE_WIN;
        } else {
            gameResult = GameResult.BLACK_WIN;
        }

        this.isGameOver = StatusPair.ofTrue(gameResult);
        calculatePlayersRating();
    }

    private void calculatePlayersRating() {
        playerForWhite.changeRating(this);
        playerForBlack.changeRating(this);
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

    @Override
    public String toString() {
        return String.format("""
                ChessGame {
                Id : %s,
                Players turn : %s,
                Player for white figures : %s,
                Player for black figures : %s,
                Rating of player for white : %d,
                Rating of player for black : %d,
                Creation date : %s,
                Last Updated Date : %s.
                TimeControllingType : %s,
                Is game over : %s, reason : %s
                }
                """,
                this.chessGameId.toString(), this.playersTurn.toString(), this.playerForWhite.getUsername(), this.playerForBlack.getUsername(),
                this.playerForWhiteRating.rating(), this.playerForBlackRating.rating(), this.sessionEvents.creationDate().toString(),
                this.sessionEvents.lastUpdateDate().toString(), this.timeControllingTYPE.toString(),
                isGameOver.status(), isGameOver.status() ? isGameOver.orElseThrow().toString() : "game is not over."
        );
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
