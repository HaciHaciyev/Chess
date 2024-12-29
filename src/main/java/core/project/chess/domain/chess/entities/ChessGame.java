package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.entities.ChessBoard.Operations;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.enumerations.GameResultMessage;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.util.ChessCountdownTimer;
import core.project.chess.domain.chess.value_objects.ChatMessage;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Rating;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.annotation.Nullable;
import lombok.Getter;

import java.time.Duration;
import java.util.*;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.enumerations.GameResultMessage.*;

@Getter
public class ChessGame {
    private final UUID chessGameId;
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating playerForWhiteRating;
    private final Rating playerForBlackRating;
    private final SessionEvents sessionEvents;
    private final Time time;
    private final List<ChatMessage> chatMessages;
    private final boolean isCasualGame;
    private final ChessCountdownTimer whiteTimer;
    private final ChessCountdownTimer blackTimer;

    private Color playersTurn;
    private boolean lastMoveWasUndo;
    private boolean isThreeFoldActive;
    private AgreementPair agreementPair;
    private AgreementPair returnOfMovement;
    private StatusPair<GameResult> isGameOver;

    private ChessGame(UUID chessGameId,
                      ChessBoard chessBoard,
                      UserAccount playerForWhite,
                      UserAccount playerForBlack,
                      Rating playerForWhiteRating,
                      Rating playerForBlackRating,
                      SessionEvents sessionEvents,
                      Time time,
                      StatusPair<GameResult> statusPair,
                      boolean isCasualGame) {

        Objects.requireNonNull(chessGameId);
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(playerForWhite);
        Objects.requireNonNull(playerForBlack);
        Objects.requireNonNull(playerForWhiteRating);
        Objects.requireNonNull(playerForBlackRating);
        Objects.requireNonNull(sessionEvents);
        Objects.requireNonNull(time);
        Objects.requireNonNull(statusPair);

        if (playerForBlack.getId().equals(playerForWhite.getId())) {
            throw new IllegalArgumentException("Game can`t be initialize with one same player.");
        }

        this.chessGameId = chessGameId;
        this.agreementPair = new AgreementPair(null, null);
        this.returnOfMovement = new AgreementPair(null, null);
        this.lastMoveWasUndo = false;
        this.chessBoard = chessBoard;
        this.playersTurn = WHITE;
        this.isThreeFoldActive = false;
        this.playerForWhite = playerForWhite;
        this.playerForBlack = playerForBlack;
        this.playerForWhiteRating = playerForWhiteRating;
        this.playerForBlackRating = playerForBlackRating;
        this.sessionEvents = sessionEvents;
        this.time = time;
        this.isGameOver = statusPair;
        this.chatMessages = new ArrayList<>();
        this.isCasualGame = isCasualGame;

        this.whiteTimer = new ChessCountdownTimer(this, "White timer", Duration.ofMinutes(time.getMinutes()),
                () -> this.isGameOver = StatusPair.ofTrue(GameResult.BLACK_WIN));

        this.blackTimer = new ChessCountdownTimer(this, "Black timer", Duration.ofMinutes(time.getMinutes()),
                () -> this.isGameOver = StatusPair.ofTrue(GameResult.WHITE_WIN));

        playerForWhite.addGame(this);
        playerForBlack.addGame(this);
    }

    public static ChessGame of(
            UUID chessGameId,
            ChessBoard chessBoard,
            UserAccount playerForWhite,
            UserAccount playerForBlack,
            SessionEvents sessionEvents,
            Time time,
            boolean isCasualGame
    ) {
        return new ChessGame(
                chessGameId,
                chessBoard,
                playerForWhite,
                playerForBlack,
                playerForWhite.getRating(),
                playerForBlack.getRating(),
                sessionEvents,
                time,
                StatusPair.ofFalse(),
                isCasualGame
        );
    }

    public void addChatMessage(final String username, final ChatMessage message) {
        validateUsername(username);
        chatMessages.add(message);
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
        if (playersTurn.equals(WHITE)) {
            playersTurn = BLACK;
            whiteTimer.pause();
            blackTimer.start();
        } else {
            playersTurn = WHITE;
            blackTimer.pause();
            whiteTimer.start();
        }
    }

    public boolean isAgreementAvailable() {
        return agreementPair.whitePlayerUsername != null && agreementPair.blackPlayerUsername != null;
    }

    public GameResultMessage makeMovement(final String username, final Coordinate from,final Coordinate to, @Nullable Piece inCaseOfPromotion)
            throws IllegalArgumentException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (isGameOver.status()) {
            throw new IllegalStateException("Game is over by %s".formatted(isGameOver.orElseThrow()));
        }

        Color color = validateUsername(username);
        validateMovesTurn(color);

        final GameResultMessage message = chessBoard.reposition(from, to, inCaseOfPromotion);

        this.lastMoveWasUndo = false;
        this.isThreeFoldActive = message.equals(GameResultMessage.RuleOf3EqualsPositions);

        resetAgreements();

        if (isGameOverMessage(message)) {
            whiteTimer.stop();
            blackTimer.stop();

            if (message.equals(Checkmate)) {
                gameOver(Operations.CHECKMATE);
            }

            if (message.equals(Stalemate) || message.equals(RuleOf50Moves) || message.equals(InsufficientMatingMaterial)) {
                gameOver(Operations.STALEMATE);
            }

            return message;
        }

        switchPlayersTurn();
        return message;
    }

    public void returnMovement(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (attemptToUndoMovement(color)) {
            this.lastMoveWasUndo = true;
            this.returnOfMovement = new AgreementPair(null, null);

            if (!chessBoard.returnOfTheMovement()) {
                throw new IllegalArgumentException("Can`t return the move.");
            }

            switchPlayersTurn();
            return;
        }

        setReturnOfMovementAgreement(color, username);
    }

    private boolean attemptToUndoMovement(Color color) {
        if (color.equals(WHITE)) {
            return Objects.nonNull(returnOfMovement.blackPlayerUsername());
        } else {
            return Objects.nonNull(returnOfMovement.whitePlayerUsername());
        }
    }

    private void setReturnOfMovementAgreement(Color color, String username) {
        if (color.equals(WHITE)) {
            this.returnOfMovement = new AgreementPair(username, null);
        } else {
            this.returnOfMovement = new AgreementPair(null, username);
        }
    }

    public void resignation(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (color.equals(WHITE)) {
            this.isGameOver = StatusPair.ofTrue(GameResult.BLACK_WIN);
            calculatePlayersRating();
            return;
        }

        this.isGameOver = StatusPair.ofTrue(GameResult.WHITE_WIN);
        calculatePlayersRating();
    }

    public void endGameByThreeFold(final String username) {
        Objects.requireNonNull(username);
        validateUsername(username);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (!isThreeFoldActive) {
            return;
        }

        gameOver(Operations.STALEMATE);
    }

    public void agreement(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (attemptToFinalizeAgreement(color)) {
            this.agreementPair = new AgreementPair(playerForWhite.getUsername().username(), playerForBlack.getUsername().username());
            gameOver(Operations.STALEMATE);
            return;
        }

        setAgreement(color, username);
    }

    private boolean attemptToFinalizeAgreement(Color color) {
        if (color.equals(WHITE)) {
            return Objects.nonNull(agreementPair.blackPlayerUsername());
        } else {
            return Objects.nonNull(agreementPair.whitePlayerUsername());
        }
    }

    private void setAgreement(Color color, String username) {
        if (color.equals(WHITE)) {
            this.agreementPair = new AgreementPair(username, null);
        } else {
            this.agreementPair = new AgreementPair(null, username);
        }
    }


    private void gameOver(final Operations operation) {
        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game was over.");
        }

        if (operation.equals(Operations.STALEMATE)) {
            drawEnding();
            return;
        }

        if (operation.equals(Operations.CHECKMATE)) {
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
        if (playersTurn.equals(WHITE)) {
            gameResult = GameResult.WHITE_WIN;
        } else {
            gameResult = GameResult.BLACK_WIN;
        }

        this.isGameOver = StatusPair.ofTrue(gameResult);
        calculatePlayersRating();
    }

    private void calculatePlayersRating() {
        if (this.isCasualGame) {
            return;
        }

        playerForWhite.changeRating(this);
        playerForBlack.changeRating(this);
    }

    public Duration remainingTimeForWhite() {
        return whiteTimer.remainingTime();
    }

    public Duration remainingTimeForBlack() {
        return blackTimer.remainingTime();
    }

    private Color validateUsername(final String username) {
        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalArgumentException("Not a player.");
        }

        return isWhitePlayer ? WHITE : BLACK;
    }

    private void validateMovesTurn(Color color) {
        final boolean whiteTriesToMoveButNotHisTurn = color.equals(WHITE) && !WHITE.equals(playersTurn);
        if (whiteTriesToMoveButNotHisTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }

        final boolean blackTriesToMoveButNotHistTurn = color.equals(BLACK) && !BLACK.equals(playersTurn);
        if (blackTriesToMoveButNotHistTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }
    }

    private void resetAgreements() {
        this.agreementPair = new AgreementPair(null, null);
        this.returnOfMovement = new AgreementPair(null, null);
    }

    private boolean isGameOverMessage(GameResultMessage message) {
        return message.equals(GameResultMessage.Checkmate) ||
                message.equals(GameResultMessage.Stalemate) ||
                message.equals(GameResultMessage.RuleOf50Moves) ||
                message.equals(GameResultMessage.InsufficientMatingMaterial);
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
                time == chessGame.time &&
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
        result = 31 * result + Objects.hashCode(time);
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
                Rating of player for white : %f,
                Rating of player for black : %f,
                Creation date : %s,
                Last Updated Date : %s.
                TimeControllingType : %s,
                Is Game Casual: %s.
                Is game over : %s, reason : %s
                }
                """,
                this.chessGameId.toString(), this.playersTurn.toString(), this.playerForWhite.getUsername(), this.playerForBlack.getUsername(),
                this.playerForWhiteRating.rating(), this.playerForBlackRating.rating(), this.sessionEvents.creationDate().toString(),
                this.sessionEvents.lastUpdateDate().toString(), this.time.toString(), this.isCasualGame,
                isGameOver.status(), isGameOver.status() ? isGameOver.orElseThrow().toString() : "game is not over."
        );
    }

    @Getter
    public enum Time {
        BULLET(1),
        BLITZ(5),
        RAPID(10),
        CLASSIC(30),
        DEFAULT(180);

        private final int minutes;

        Time(int minutes) {
            this.minutes = minutes;
        }
    }

    public record AgreementPair(String whitePlayerUsername, String blackPlayerUsername) {}
}
