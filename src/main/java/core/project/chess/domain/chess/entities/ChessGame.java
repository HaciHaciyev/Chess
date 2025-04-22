package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.entities.ChessBoard.Operations;
import core.project.chess.domain.chess.enumerations.*;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.util.ChessCountdownTimer;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.ChatMessage;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.value_objects.Rating;
import core.project.chess.domain.user.value_objects.Username;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.*;

import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.enumerations.GameResultMessage.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ChessGame {
    private final UUID chessGameId;
    private final ChessBoard chessBoard;
    private final User whitePlayer;
    private final User blackPlayer;
    private final Rating whiteRating;
    private final Rating blackRating;
    private final SessionEvents sessionEvents;
    private final Time time;
    private final List<ChatMessage> chatMessages;
    private final boolean isCasualGame;
    private final ChessCountdownTimer whiteTimer;
    private final ChessCountdownTimer blackTimer;

    private Color playersTurn;
    private boolean isThreeFoldActive;
    private AgreementPair agreementPair;
    private AgreementPair returnOfMovement;
    private ChessCountdownTimer afkTimer;
    private GameResult isGameOver;

    public static final int TIME_FOR_AFK = 45;

    private ChessGame(UUID chessGameId,
                      ChessBoard chessBoard,
                      User whitePlayer,
                      User blackPlayer,
                      Rating whiteRating,
                      Rating blackRating,
                      SessionEvents sessionEvents,
                      Time time,
                      GameResult gameResult,
                      boolean isCasualGame) {

        Objects.requireNonNull(chessGameId);
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(whitePlayer);
        Objects.requireNonNull(blackPlayer);
        Objects.requireNonNull(whiteRating);
        Objects.requireNonNull(blackRating);
        Objects.requireNonNull(sessionEvents);
        Objects.requireNonNull(time);
        Objects.requireNonNull(gameResult);

        if (blackPlayer.id().equals(whitePlayer.id()))
            throw new IllegalArgumentException("Game can`t be initialized with same player for both sides.");

        this.chessGameId = chessGameId;
        this.agreementPair = new AgreementPair(null, null);
        this.returnOfMovement = new AgreementPair(null, null);
        this.chessBoard = chessBoard;
        this.playersTurn = this.chessBoard.turn();
        this.isThreeFoldActive = false;
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.whiteRating = whiteRating;
        this.blackRating = blackRating;
        this.sessionEvents = sessionEvents;
        this.time = time;
        this.isGameOver = gameResult;
        this.chatMessages = new ArrayList<>();
        this.isCasualGame = isCasualGame;

        this.whiteTimer = new ChessCountdownTimer(this, "White timer", Duration.ofMinutes(time.getMinutes()), () -> {
            this.isGameOver = GameResult.BLACK_WIN;
            calculatePlayersRating();
        });

        this.blackTimer = new ChessCountdownTimer(this, "Black timer", Duration.ofMinutes(time.getMinutes()), () -> {
            this.isGameOver = GameResult.WHITE_WIN;
            calculatePlayersRating();
        });

        whitePlayer.addGame(this);
        blackPlayer.addGame(this);
    }

    public static ChessGame standard(
            UUID chessGameId,
            User whitePlayer,
            User blackPlayer,
            SessionEvents sessionEvents,
            Time time,
            boolean isCasualGame
    ) {
        Rating whiteRating = getRating(whitePlayer, time);
        Rating blackRating = getRating(blackPlayer, time);

        return chessGameInit(chessGameId, ChessBoard.starndardChessBoard(), whitePlayer, blackPlayer,
                whiteRating, blackRating, sessionEvents, time, isCasualGame);
    }

    public static ChessGame byPGN(
            UUID chessGameId,
            String pgn,
            User whitePlayer,
            User blackPlayer,
            SessionEvents sessionEvents,
            Time time,
            Boolean isCasualGame) {

        Rating whiteRating = getRating(whitePlayer, time);
        Rating blackRating = getRating(blackPlayer, time);

        return chessGameInit(chessGameId, ChessBoard.fromPGN(pgn), whitePlayer, blackPlayer,
                whiteRating, blackRating, sessionEvents, time, isCasualGame);
    }

    public static ChessGame byFEN(
            UUID chessGameId,
            String fen,
            User whitePlayer,
            User blackPlayer,
            SessionEvents sessionEvents,
            Time time,
            Boolean isCasualGame) {

        Rating whiteRating = getRating(whitePlayer, time);
        Rating blackRating = getRating(blackPlayer, time);

        return chessGameInit(chessGameId, ChessBoard.fromPosition(fen), whitePlayer, blackPlayer,
                whiteRating, blackRating, sessionEvents, time, isCasualGame);
    }

    public static ChessGame pureChess(
            UUID chessGameId,
            User whitePlayer,
            User blackPlayer,
            SessionEvents sessionEvents,
            Time time,
            boolean isCasualGame) {

        Rating whiteRating = getRating(whitePlayer, time);
        Rating blackRating = getRating(blackPlayer, time);

        return chessGameInit(chessGameId, ChessBoard.pureChess(), whitePlayer, blackPlayer,
                whiteRating, blackRating, sessionEvents, time, isCasualGame);
    }

    public static ChessGame pureChessByFEN(
            UUID chessGameId,
            String fen,
            User whitePlayer,
            User blackPlayer,
            SessionEvents sessionEvents,
            Time time,
            boolean isCasualGame) {

        Rating whiteRating = getRating(whitePlayer, time);
        Rating blackRating = getRating(blackPlayer, time);

        return chessGameInit(chessGameId, ChessBoard.pureChessFromPosition(fen), whitePlayer, blackPlayer,
                whiteRating, blackRating, sessionEvents, time, isCasualGame);
    }

    private static ChessGame chessGameInit(
            UUID chessGameId,
            ChessBoard chessBoard,
            User whitePlayer,
            User blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            SessionEvents sessionEvents,
            Time time,
            boolean isCasualGame) {

        return new ChessGame(
                chessGameId,
                chessBoard,
                whitePlayer,
                blackPlayer,
                whiteRating,
                blackRating,
                sessionEvents,
                time,
                GameResult.NONE,
                isCasualGame
        );
    }

    private static Rating getRating(User user, Time time) {
        return switch (time) {
            case DEFAULT, CLASSIC -> user.rating();
            case BULLET -> user.bulletRating();
            case BLITZ -> user.blitzRating();
            case RAPID -> user.rapidRating();
        };
    }

    public UUID chessGameID() {
        return chessGameId;
    }

    public String fen() {
        return chessBoard.toString();
    }

    public String pgn() {
        return chessBoard.pgn();
    }

    public List<String> listOfAlgebraicNotations() {
        return chessBoard.listOfAlgebraicNotations();
    }

    public Optional<AlgebraicNotation> lastAlgebraicNotation() {
        return chessBoard.lastAlgebraicNotation();
    }

    public int countOfHalfMoves() {
        return chessBoard.countOfHalfMoves();
    }

    public int countOfFullMoves() {
        return chessBoard.countOfFullMoves();
    }

    public UUID historyID() {
        return chessBoard.ID();
    }

    public User whitePlayer() {
        return whitePlayer;
    }

    public User blackPlayer() {
        return blackPlayer;
    }

    public Rating whiteRating() {
        return whiteRating;
    }

    public Rating blackRating() {
        return blackRating;
    }

    public SessionEvents sessionEvents() {
        return sessionEvents;
    }

    public Time time() {
        return time;
    }

    public Color playersTurn() {
        return playersTurn;
    }

    public boolean isThreeFoldActive() {
        return isThreeFoldActive;
    }

    public boolean isCasualGame() {
        return isCasualGame;
    }

    public ToStringUtils toStringUtils() {
        return new ToStringUtils(chessBoard);
    }

    public void addChatMessage(final String username, final ChatMessage message) {
        validateUsername(username);
        chatMessages.add(message);
    }

    public GameResult gameResult() {
        return isGameOver;
    }

    public boolean isGameOver() {
        return isGameOver != GameResult.NONE;
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

    public boolean isPlayer(Username username) {
        return username.username().equals(whitePlayer.username()) || username.username().equals(blackPlayer.username());
    }

    public GameResultMessage doMove(
            final String username,
            final Coordinate from,
            final Coordinate to,
            final @Nullable Piece inCaseOfPromotion) throws IllegalArgumentException {

        Objects.requireNonNull(username);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (!chessBoard.isPureChess() && isGameOver != GameResult.NONE)
            throw new IllegalStateException("Game is over by %s".formatted(isGameOver));

        Color color = validateUsername(username);
        validateMovesTurn(color);

        final GameResultMessage message = chessBoard.doMove(from, to, inCaseOfPromotion);

        this.isThreeFoldActive = message.equals(GameResultMessage.RuleOf3EqualsPositions);

        resetAgreements();

        if (isGameOverMessage(message)) {
            whiteTimer.stop();
            blackTimer.stop();

            if (message.equals(Checkmate)) gameOver(Operations.CHECKMATE);
            if (message.equals(Stalemate) || message.equals(RuleOf50Moves) || message.equals(InsufficientMatingMaterial))
                gameOver(Operations.STALEMATE);

            return message;
        }

        switchPlayersTurn();
        return message;
    }

    public UndoMoveResult undo(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (!chessBoard.isPureChess() && isGameOver != GameResult.NONE)
            throw new IllegalArgumentException("Game is over.");

        if (attemptToUndoMovement(color)) {
            this.returnOfMovement = new AgreementPair(null, null);
            if (!chessBoard.undoMove()) return UndoMoveResult.FAILED_UNDO;

            switchPlayersTurn();
            if (chessBoard.isThreeFoldActive()) this.isThreeFoldActive = true;
            return UndoMoveResult.SUCCESSFUL_UNDO;
        }

        setReturnOfMovementAgreement(color, username);
        return UndoMoveResult.UNDO_REQUESTED;
    }

    private boolean attemptToUndoMovement(Color color) {
        if (color.equals(WHITE)) return nonNull(returnOfMovement.blackPlayerUsername());
        else return nonNull(returnOfMovement.whitePlayerUsername());
    }

    private void setReturnOfMovementAgreement(Color color, String username) {
        if (color.equals(WHITE)) this.returnOfMovement = new AgreementPair(username, null);
        else this.returnOfMovement = new AgreementPair(null, username);
    }

    public void resignation(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");

        if (color.equals(WHITE)) {
            this.isGameOver = GameResult.BLACK_WIN;
            calculatePlayersRating();
            return;
        }

        this.isGameOver = GameResult.WHITE_WIN;
        calculatePlayersRating();
    }

    public void endGameByThreeFold(final String username) {
        Objects.requireNonNull(username);
        validateUsername(username);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");
        if (!isThreeFoldActive) return;
        gameOver(Operations.STALEMATE);
    }

    public void agreement(final String username) {
        Objects.requireNonNull(username);
        Color color = validateUsername(username);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");

        if (attemptToFinalizeAgreement(color)) {
            this.agreementPair = new AgreementPair(whitePlayer.username(), blackPlayer.username());
            gameOver(Operations.STALEMATE);
            return;
        }

        setAgreement(color, username);
    }

    public void awayFromTheBoard(Username username) {
        if (!isPlayer(username)) return;
        if (nonNull(afkTimer)) return;

        Color color = username.username().equals(whitePlayer.username()) ? WHITE : BLACK;

        if (color == WHITE) {
            this.afkTimer = new ChessCountdownTimer(this, "AFK White timer", Duration.ofSeconds(TIME_FOR_AFK), () -> {
                this.isGameOver = GameResult.BLACK_WIN;
                calculatePlayersRating();
            });
            return;
        }

        this.afkTimer = new ChessCountdownTimer(this, "AFK Black timer", Duration.ofSeconds(TIME_FOR_AFK), () -> {
            this.isGameOver = GameResult.WHITE_WIN;
            calculatePlayersRating();
        });
    }

    public void returnedToTheBoard(Username username) {
        if (!isPlayer(username)) throw new IllegalArgumentException("Not a player: " + username);
        if (isNull(afkTimer)) return;

        afkTimer.stop();
        this.afkTimer = null;
    }

    private boolean attemptToFinalizeAgreement(Color color) {
        if (color.equals(WHITE)) return nonNull(agreementPair.blackPlayerUsername());
        else return nonNull(agreementPair.whitePlayerUsername());
    }

    private void setAgreement(Color color, String username) {
        if (color.equals(WHITE)) this.agreementPair = new AgreementPair(username, null);
        else this.agreementPair = new AgreementPair(null, username);
    }

    private void gameOver(final Operations operation) {
        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is already over.");

        if (operation.equals(Operations.STALEMATE)) {
            drawEnding();
            return;
        }

        if (operation.equals(Operations.CHECKMATE)) winnerEnding();
    }

    private void drawEnding() {
        this.isGameOver = GameResult.DRAW;
        calculatePlayersRating();
    }

    private void winnerEnding() {
        GameResult gameResult;
        if (playersTurn.equals(WHITE)) gameResult = GameResult.WHITE_WIN;
        else gameResult = GameResult.BLACK_WIN;

        this.isGameOver = gameResult;
        calculatePlayersRating();
    }

    private void calculatePlayersRating() {
        if (this.isCasualGame) return;
        whitePlayer.changeRating(this);
        blackPlayer.changeRating(this);
    }

    public Duration remainingTimeForWhite() {
        return whiteTimer.remainingTime();
    }

    public Duration remainingTimeForBlack() {
        return blackTimer.remainingTime();
    }

    private Color validateUsername(final String username) {
        final boolean isWhitePlayer = username.equals(whitePlayer.username());
        final boolean isBlackPlayer = username.equals(blackPlayer.username());

        if (!isWhitePlayer && !isBlackPlayer) throw new IllegalArgumentException("Not a player: " + username);
        if (nonNull(afkTimer)) {
            final boolean illegalAccess = (isWhitePlayer && afkTimer.name().equals("AFK White timer")) ||
                    (isBlackPlayer && afkTimer.name().equals("AFK Black timer"));

            if (illegalAccess)
                throw new IllegalStateException("A player cannot make a move without being at the board: " + username);
        }

        return isWhitePlayer ? WHITE : BLACK;
    }

    private void validateMovesTurn(Color color) {
        final boolean whiteTriesToMoveButNotHisTurn = color.equals(WHITE) && !WHITE.equals(playersTurn);
        if (whiteTriesToMoveButNotHisTurn) throw new IllegalArgumentException("It`s black move turn.");

        final boolean blackTriesToMoveButNotHistTurn = color.equals(BLACK) && !BLACK.equals(playersTurn);
        if (blackTriesToMoveButNotHistTurn) throw new IllegalArgumentException("It`s white move turn.");
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
                Objects.equals(whitePlayer, chessGame.whitePlayer) &&
                Objects.equals(blackPlayer, chessGame.blackPlayer) &&
                Objects.equals(whiteRating, chessGame.whiteRating) &&
                Objects.equals(blackRating, chessGame.blackRating) &&
                Objects.equals(sessionEvents, chessGame.sessionEvents) &&
                time == chessGame.time &&
                Objects.equals(isGameOver, chessGame.isGameOver);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chessBoard);
        result = 31 * result + Objects.hashCode(whitePlayer);
        result = 31 * result + Objects.hashCode(blackPlayer);
        result = 31 * result + Objects.hashCode(whiteRating);
        result = 31 * result + Objects.hashCode(blackRating);
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
                this.chessGameId.toString(), this.playersTurn.toString(), this.whitePlayer.username(),
                this.blackPlayer.username(), this.whiteRating.rating(), this.blackRating.rating(),
                this.sessionEvents.creationDate().toString(), this.sessionEvents.lastUpdateDate().toString(),
                this.time.toString(), this.isCasualGame, isGameOver != GameResult.NONE, isGameOver
        );
    }

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

        public int getMinutes() {
            return minutes;
        }
    }

    private record AgreementPair(String whitePlayerUsername, String blackPlayerUsername) {}
}
