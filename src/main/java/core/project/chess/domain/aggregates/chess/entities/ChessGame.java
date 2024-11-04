package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.enumerations.GameResult;
import core.project.chess.domain.aggregates.chess.enumerations.GameResultMessage;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.pieces.Piece;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import io.quarkus.logging.Log;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static core.project.chess.domain.aggregates.chess.enumerations.GameResultMessage.*;

@Getter
public class ChessGame {
    private final UUID chessGameId;
    private AgreementPair agreementPair;
    private AgreementPair returnOfMovement;
    private boolean lastMoveWasUndo;
    private @Getter(AccessLevel.NONE) Color playersTurn;
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Rating playerForWhiteRating;
    private final Rating playerForBlackRating;
    private final SessionEvents sessionEvents;
    private final TimeControllingTYPE timeControllingTYPE;
    private @Getter(AccessLevel.NONE) boolean isTheOptionToEndTheGameDueToThreeFoldActive;
    private @Getter(AccessLevel.NONE) StatusPair<GameResult> isGameOver;
    final @Getter(AccessLevel.NONE) List<Message> chatMessages;

    private final ChessCountdownTimer whiteTimer;
    private final ChessCountdownTimer blackTimer;

    private ChessGame(UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite, UserAccount playerForBlack,
                      Rating playerForWhiteRating, Rating playerForBlackRating, SessionEvents sessionEvents,
                      TimeControllingTYPE timeControllingTYPE, StatusPair<GameResult> statusPair) {

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
        this.agreementPair = new AgreementPair(null, null);
        this.returnOfMovement = new AgreementPair(null, null);
        this.lastMoveWasUndo = false;
        this.chessBoard = chessBoard;
        this.playersTurn = Color.WHITE;
        this.isTheOptionToEndTheGameDueToThreeFoldActive = false;
        this.playerForWhite = playerForWhite;
        this.playerForBlack = playerForBlack;
        this.playerForWhiteRating = playerForWhiteRating;
        this.playerForBlackRating = playerForBlackRating;
        this.sessionEvents = sessionEvents;
        this.timeControllingTYPE = timeControllingTYPE;
        this.isGameOver = statusPair;
        this.chatMessages = new ArrayList<>();

        this.whiteTimer = new ChessCountdownTimer("White timer", Duration.ofMinutes(timeControllingTYPE.getMinutes()),
                unused -> this.isGameOver = StatusPair.ofTrue(GameResult.BLACK_WIN));

        this.blackTimer = new ChessCountdownTimer("Black timer", Duration.ofMinutes(timeControllingTYPE.getMinutes()),
                unused -> this.isGameOver = StatusPair.ofTrue(GameResult.WHITE_WIN));

        playerForWhite.addGame(this);
        playerForBlack.addGame(this);
    }

    public static ChessGame fromRepository(UUID chessGameId, ChessBoard chessBoard, UserAccount playerForWhite,
                                           UserAccount playerForBlack, Rating whitePlayerRating, Rating blackPlayerRating,
                                           SessionEvents sessionEvents, TimeControllingTYPE timeControllingTYPE, StatusPair<GameResult> statusPair
    ) {
        return new ChessGame(
                chessGameId, chessBoard, playerForWhite, playerForBlack, whitePlayerRating, blackPlayerRating, sessionEvents, timeControllingTYPE, statusPair
        );
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

    public void addChatMessage(final String username, final Message message) {
        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalArgumentException("Not a player.");
        }

        chatMessages.add(message);
    }

    public List<Message> chatMessages() {
        return chatMessages.stream().toList();
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
            whiteTimer.pause();
            blackTimer.start();
        } else {
            playersTurn = Color.WHITE;
            blackTimer.pause();
            whiteTimer.start();
        }
    }

    public boolean isAgreementAvailable() {
        return agreementPair.whitePlayerUsername != null && agreementPair.blackPlayerUsername != null;
    }

    public boolean isMoveReturningAgreed() {
        return returnOfMovement.whitePlayerUsername != null && returnOfMovement.blackPlayerUsername != null;
    }

    public GameResultMessage makeMovement(final String username, final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion)
            throws IllegalArgumentException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (isGameOver.status()) {
            throw new IllegalStateException("Game is over by %s".formatted(isGameOver.orElseThrow()));
        }

        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        final boolean thirdPartyUser = !isWhitePlayer && !isBlackPlayer;
        if (thirdPartyUser) {
            throw new IllegalArgumentException("Not a player.");
        }

        final boolean whiteTriesToMoveButNotHisTurn = isWhitePlayer && !Color.WHITE.equals(playersTurn);
        if (whiteTriesToMoveButNotHisTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }

        final boolean blackTriesToMoveButNotHistTurn = isBlackPlayer && !Color.BLACK.equals(playersTurn);
        if (blackTriesToMoveButNotHistTurn) {
            throw new IllegalArgumentException("It`s opponent move turn.");
        }

        final GameResultMessage message = chessBoard.reposition(from, to, inCaseOfPromotion);

        lastMoveWasUndo = false;

        if (message.equals(GameResultMessage.RuleOf3EqualsPositions)) {
            isTheOptionToEndTheGameDueToThreeFoldActive = true;
        } else {
            isTheOptionToEndTheGameDueToThreeFoldActive = false;
        }

        this.agreementPair = new AgreementPair(null, null);
        this.returnOfMovement = new AgreementPair(null, null);

        final boolean gameOver =
                message.equals(Checkmate) || message.equals(Stalemate) || message.equals(RuleOf50Moves) || message.equals(InsufficientMatingMaterial);

        if (gameOver) {
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

    public boolean returnMovement(final String username) {
        Objects.requireNonNull(username);

        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalArgumentException("Not a player.");
        }

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (isWhitePlayer) {

            final boolean playerForBlackIsAlreadyAgreed =
                    !Objects.isNull(returnOfMovement.blackPlayerUsername()) && returnOfMovement.blackPlayerUsername().equals(playerForBlack.getUsername().username());
            if (playerForBlackIsAlreadyAgreed) {
                lastMoveWasUndo = true;
                this.returnOfMovement = new AgreementPair(null, null);

                final boolean successfulMoveReturning = chessBoard.returnOfTheMovement();
                if (!successfulMoveReturning) {
                    throw new IllegalArgumentException("Can`t return the move.");
                }

                switchPlayersTurn();
                return true;
            }

            this.returnOfMovement = new AgreementPair(username, null);
            return false;
        }

        final boolean playerForWhiteIsAlreadyAgreed =
                !Objects.isNull(returnOfMovement.whitePlayerUsername()) && returnOfMovement.whitePlayerUsername().equals(playerForWhite.getUsername().username());
        if (playerForWhiteIsAlreadyAgreed) {
            lastMoveWasUndo = true;
            this.returnOfMovement = new AgreementPair(null, null);

            final boolean successfulMoveReturning = chessBoard.returnOfTheMovement();
            if (!successfulMoveReturning) {
                throw new IllegalArgumentException("Can`t return the move.");
            }

            switchPlayersTurn();
            return true;
        }

        this.returnOfMovement = new AgreementPair(null, username);
        return false;
    }

    public void resignation(final String username) {
        Objects.requireNonNull(username);
        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (isWhitePlayer) {
            this.isGameOver = StatusPair.ofTrue(GameResult.BLACK_WIN);
            calculatePlayersRating();
            return;
        }

        if (isBlackPlayer) {
            this.isGameOver = StatusPair.ofTrue(GameResult.WHITE_WIN);
            calculatePlayersRating();
            return;
        }

        throw new IllegalArgumentException("Not a player.");
    }

    public boolean endGameByThreeFold(final String username) {
        Objects.requireNonNull(username);

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (!username.equals(playerForWhite.getUsername().username()) || !username.equals(playerForBlack.getUsername().username())) {
            throw new IllegalArgumentException("Not legal user access.");
        }

        if (!isTheOptionToEndTheGameDueToThreeFoldActive) {
            return false;
        }

        gameOver(Operations.STALEMATE);
        return true;
    }

    public boolean agreement(final String username) {
        Objects.requireNonNull(username);
        final boolean isWhitePlayer = username.equals(playerForWhite.getUsername().username());
        final boolean isBlackPlayer = username.equals(playerForBlack.getUsername().username());
        if (!isWhitePlayer && !isBlackPlayer) {
            throw new IllegalArgumentException("Not a player.");
        }

        if (isGameOver.status()) {
            throw new IllegalArgumentException("Game is over.");
        }

        if (isWhitePlayer) {

            final boolean playerForBlackIsAlreadyAgreed =
                    !Objects.isNull(agreementPair.blackPlayerUsername()) && agreementPair.blackPlayerUsername().equals(playerForBlack.getUsername().username());
            if (playerForBlackIsAlreadyAgreed) {
                this.agreementPair = new AgreementPair(playerForWhite.getUsername().username(), playerForBlack.getUsername().username());
                gameOver(Operations.STALEMATE);

                return true;
            }

            this.agreementPair = new AgreementPair(username, null);
            return false;
        }

        final boolean playerForWhiteIsAlreadyAgreed =
                !Objects.isNull(agreementPair.whitePlayerUsername()) && agreementPair.whitePlayerUsername().equals(playerForWhite.getUsername().username());
        if (playerForWhiteIsAlreadyAgreed) {
            this.agreementPair = new AgreementPair(playerForWhite.getUsername().username(), playerForBlack.getUsername().username());
            gameOver(Operations.STALEMATE);

            return true;
        }

        this.agreementPair = new AgreementPair(null, username);
        return false;
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
                Rating of player for white : %f,
                Rating of player for black : %f,
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

    public record AgreementPair(String whitePlayerUsername, String blackPlayerUsername) {}

    private class ChessCountdownTimer implements Runnable {

        private Instant startTime;
        private Instant pauseTime;
        private final Duration gameDuration;

        private final AtomicBoolean isPaused;
        private final AtomicBoolean isRunning;

        private final Object lock;
        private final ExecutorService timerService;

        private final Consumer<Void> onComplete;

        private final String name;

        public ChessCountdownTimer(String name, Duration duration, Consumer<Void> onComplete) {
            this.name = name;
            this.gameDuration = duration;
            this.isPaused = new AtomicBoolean();
            this.isRunning = new AtomicBoolean();
            this.lock = new Object();
            this.timerService = Executors.newSingleThreadExecutor();
            this.onComplete = onComplete;
        }

        @Override
        public void run() {
            try {
                while (isRunning.get()) {
                    synchronized (lock) {
                        if (isPaused.get()) {
                            lock.wait();
                        }
                    }

                    Duration elapsed = Duration.between(startTime, Instant.now());
                    Duration remaining = gameDuration.minus(elapsed);

                    if (remaining.isNegative() || remaining.isZero()) {
                        onComplete.accept(null);
                        stop();
                        break;
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public boolean start() {
            if (isRunning.get() && !isPaused.get()) {
                Log.warnf("%s for the game %s is already running", name, chessGameId);
                return false;
            }

            if (!isPaused.get()) {
                Log.infof("starting {%s}", name);
            }

            isRunning.set(true);

            if (isPaused.get()) {
                Log.infof("resuming {%s}", name);
                Duration pauseDuration = Duration.between(pauseTime, Instant.now());
                startTime = startTime.plus(pauseDuration);
                isPaused.set(false);

                synchronized (lock) {
                    lock.notify();
                }

                return true;
            } else {
                startTime = Instant.now();
            }

            timerService.submit(this);
            return true;
        }

        public void pause() {
            if (isRunning.get() && !isPaused.get()) {
                Log.infof("pausing {%s}...", name);
                pauseTime = Instant.now();
                isPaused.set(true);
            }
        }

        public void stop() {
            isRunning.set(false);
            timerService.shutdownNow();
            Log.infof("Shutting down {%s}...", name);

            try {
                if (!timerService.awaitTermination(1, TimeUnit.SECONDS)) {
                    Log.warn("Timer service didn't terminate gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
