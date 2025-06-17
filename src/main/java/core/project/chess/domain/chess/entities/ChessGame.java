package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.entities.ChessBoard.Operations;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResultMessage;
import core.project.chess.domain.chess.enumerations.UndoMoveResult;
import core.project.chess.domain.chess.events.ChessGameResult;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.util.ChessCountdownTimer;
import core.project.chess.domain.chess.util.ToStringUtils;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.ChatMessage;
import core.project.chess.domain.chess.value_objects.GameDates;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.enumerations.Color;
import core.project.chess.domain.commons.value_objects.GameResult;
import core.project.chess.domain.commons.value_objects.Rating;
import core.project.chess.domain.commons.value_objects.RatingType;

import java.time.Duration;
import java.util.*;

import static core.project.chess.domain.chess.enumerations.GameResultMessage.*;
import static core.project.chess.domain.commons.enumerations.Color.BLACK;
import static core.project.chess.domain.commons.enumerations.Color.WHITE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ChessGame {
    private final UUID chessGameId;
    private final ChessBoard chessBoard;
    private final UUID whitePlayer;
    private final UUID blackPlayer;
    private final Rating whiteRating;
    private final Rating blackRating;
    private final GameDates gameDates;
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
    private final Deque<ChessGameResult> domainEvents = new ArrayDeque<>();

    public static final int TIME_FOR_AFK = 45;

    private ChessGame(UUID chessGameId,
                      ChessBoard chessBoard,
                      UUID whitePlayer,
                      UUID blackPlayer,
                      Rating whiteRating,
                      Rating blackRating,
                      GameDates gameDates,
                      Time time,
                      GameResult gameResult,
                      boolean isCasualGame) {

        Objects.requireNonNull(chessGameId);
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(whitePlayer);
        Objects.requireNonNull(blackPlayer);
        Objects.requireNonNull(whiteRating);
        Objects.requireNonNull(blackRating);
        Objects.requireNonNull(gameDates);
        Objects.requireNonNull(time);
        Objects.requireNonNull(gameResult);

        if (blackPlayer.equals(whitePlayer))
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
        this.gameDates = gameDates;
        this.time = time;
        this.isGameOver = gameResult;
        this.chatMessages = new ArrayList<>();
        this.isCasualGame = isCasualGame;

        this.whiteTimer = new ChessCountdownTimer(this, "White timer", Duration.ofMinutes(time.getMinutes()), () -> {
            this.isGameOver = GameResult.BLACK_WIN;
            defineGameResult();
        });

        this.blackTimer = new ChessCountdownTimer(this, "Black timer", Duration.ofMinutes(time.getMinutes()), () -> {
            this.isGameOver = GameResult.WHITE_WIN;
            defineGameResult();
        });
    }

    public static ChessGame standard(
            UUID chessGameId,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            boolean isCasualGame
    ) {

        return chessGameInit(chessGameId, ChessBoard.starndardChessBoard(), whitePlayer, blackPlayer,
                whiteRating, blackRating, gameDates, time, isCasualGame);
    }

    public static ChessGame byPGN(
            UUID chessGameId,
            String pgn,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            Boolean isCasualGame) {

        return chessGameInit(chessGameId, ChessBoard.fromPGN(pgn), whitePlayer, blackPlayer,
                whiteRating, blackRating, gameDates, time, isCasualGame);
    }

    public static ChessGame byFEN(
            UUID chessGameId,
            String fen,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            Boolean isCasualGame) {

        return chessGameInit(chessGameId, ChessBoard.fromPosition(fen), whitePlayer, blackPlayer,
                whiteRating, blackRating, gameDates, time, isCasualGame);
    }

    public static ChessGame pureChess(
            UUID chessGameId,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            boolean isCasualGame) {

        return chessGameInit(chessGameId, ChessBoard.pureChess(), whitePlayer, blackPlayer,
                whiteRating, blackRating, gameDates, time, isCasualGame);
    }

    public static ChessGame pureChessByFEN(
            UUID chessGameId,
            String fen,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            boolean isCasualGame) {

        return chessGameInit(chessGameId, ChessBoard.pureChessFromPosition(fen), whitePlayer, blackPlayer,
                whiteRating, blackRating, gameDates, time, isCasualGame);
    }

    private static ChessGame chessGameInit(
            UUID chessGameId,
            ChessBoard chessBoard,
            UUID whitePlayer,
            UUID blackPlayer,
            Rating whiteRating,
            Rating blackRating,
            GameDates gameDates,
            Time time,
            boolean isCasualGame) {

        return new ChessGame(
                chessGameId,
                chessBoard,
                whitePlayer,
                blackPlayer,
                whiteRating,
                blackRating,
                gameDates,
                time,
                GameResult.NONE,
                isCasualGame
        );
    }

    public UUID chessGameID() {
        return chessGameId;
    }

    public UUID whitePlayer() {
        return whitePlayer;
    }

    public UUID blackPlayer() {
        return blackPlayer;
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

    public Rating whiteRating() {
        return whiteRating;
    }

    public Rating blackRating() {
        return blackRating;
    }

    public GameDates sessionEvents() {
        return gameDates;
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

    public void addChatMessage(final UUID userID, final ChatMessage message) {
        validateUserID(userID);
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

    public List<ChessGameResult> pullDomainEvents() {
        List<ChessGameResult> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean isAgreementAvailable() {
        if (isGameOver()) return false;
        return agreementPair.whitePlayer() != null || agreementPair.blackPlayer() != null;
    }

    public boolean isPlayer(UUID userID) {
        return whitePlayer.equals(userID) || blackPlayer.equals(userID);
    }

    public GameResultMessage doMove(
            final UUID userID,
            final Coordinate from,
            final Coordinate to,
            final @Nullable Piece inCaseOfPromotion) throws IllegalArgumentException {

        Objects.requireNonNull(userID);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (!chessBoard.isPureChess() && isGameOver != GameResult.NONE)
            throw new IllegalStateException("Game is over by %s".formatted(isGameOver));

        Color color = validateUserID(userID);
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

    public UndoMoveResult undo(final UUID userID) {
        Objects.requireNonNull(userID);
        Color color = validateUserID(userID);

        if (!chessBoard.isPureChess() && isGameOver != GameResult.NONE)
            throw new IllegalArgumentException("Game is over.");

        if (attemptToUndoMovement(color)) {
            this.returnOfMovement = new AgreementPair(null, null);
            if (!chessBoard.undoMove()) return UndoMoveResult.FAILED_UNDO;

            switchPlayersTurn();
            if (chessBoard.isThreeFoldActive()) this.isThreeFoldActive = true;
            return UndoMoveResult.SUCCESSFUL_UNDO;
        }

        setReturnOfMovementAgreement(color, userID);
        return UndoMoveResult.UNDO_REQUESTED;
    }

    private boolean attemptToUndoMovement(Color color) {
        if (color.equals(WHITE)) return nonNull(returnOfMovement.blackPlayer());
        else return nonNull(returnOfMovement.whitePlayer());
    }

    private void setReturnOfMovementAgreement(Color color, UUID userID) {
        if (color.equals(WHITE)) this.returnOfMovement = new AgreementPair(userID, null);
        else this.returnOfMovement = new AgreementPair(null, userID);
    }

    public void resignation(final UUID userID) {
        Objects.requireNonNull(userID);
        Color color = validateUserID(userID);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");

        if (color.equals(WHITE)) {
            this.isGameOver = GameResult.BLACK_WIN;
            defineGameResult();
            return;
        }

        this.isGameOver = GameResult.WHITE_WIN;
        defineGameResult();
    }

    public void endGameByThreeFold(final UUID userID) {
        Objects.requireNonNull(userID);
        validateUserID(userID);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");
        if (!isThreeFoldActive) return;
        gameOver(Operations.STALEMATE);
    }

    public void agreement(final UUID userID) {
        Objects.requireNonNull(userID);
        Color color = validateUserID(userID);

        if (isGameOver != GameResult.NONE) throw new IllegalArgumentException("Game is over.");

        if (attemptToFinalizeAgreement(color)) {
            this.agreementPair = new AgreementPair(whitePlayer, blackPlayer);
            gameOver(Operations.STALEMATE);
            return;
        }

        setAgreement(color, userID);
    }

    public void awayFromTheBoard(UUID userID) {
        if (!isPlayer(userID)) return;
        if (nonNull(afkTimer)) return;

        Color color = userID.equals(whitePlayer) ? WHITE : BLACK;

        if (color == WHITE) {
            this.afkTimer = new ChessCountdownTimer(this, "AFK White timer", Duration.ofSeconds(TIME_FOR_AFK), () -> {
                this.isGameOver = GameResult.BLACK_WIN;
                defineGameResult();
            });
            return;
        }

        this.afkTimer = new ChessCountdownTimer(this, "AFK Black timer", Duration.ofSeconds(TIME_FOR_AFK), () -> {
            this.isGameOver = GameResult.WHITE_WIN;
            defineGameResult();
        });
    }

    public void returnedToTheBoard(UUID userID) {
        if (!isPlayer(userID)) throw new IllegalArgumentException("Not a player: " + userID);
        if (isNull(afkTimer)) return;

        afkTimer.stop();
        this.afkTimer = null;
    }

    private boolean attemptToFinalizeAgreement(Color color) {
        if (color.equals(WHITE)) return nonNull(agreementPair.blackPlayer());
        else return nonNull(agreementPair.whitePlayer());
    }

    private void setAgreement(Color color, UUID userID) {
        if (color.equals(WHITE)) this.agreementPair = new AgreementPair(userID, null);
        else this.agreementPair = new AgreementPair(null, userID);
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
        defineGameResult();
    }

    private void winnerEnding() {
        this.isGameOver = playersTurn.equals(WHITE) ? GameResult.WHITE_WIN : GameResult.BLACK_WIN;
        defineGameResult();
    }

    private void defineGameResult() {
        domainEvents.add(new ChessGameResult(chessGameId, isGameOver, whitePlayer, blackPlayer, ratingType()));
    }

    public RatingType ratingType() {
        return switch (time) {
            case DEFAULT, CLASSIC -> RatingType.CLASSIC;
            case RAPID -> RatingType.RAPID;
            case BLITZ -> RatingType.BLITZ;
            case BULLET -> RatingType.BULLET;
        };
    }

    public Duration remainingTimeForWhite() {
        return whiteTimer.remainingTime();
    }

    public Duration remainingTimeForBlack() {
        return blackTimer.remainingTime();
    }

    private Color validateUserID(final UUID userID) {
        final boolean isWhitePlayer = userID.equals(whitePlayer);
        final boolean isBlackPlayer = userID.equals(blackPlayer);

        if (!isWhitePlayer && !isBlackPlayer) throw new IllegalArgumentException("Not a player: " + userID);
        if (nonNull(afkTimer)) {
            final boolean illegalAccess = (isWhitePlayer && afkTimer.name().equals("AFK White timer")) ||
                    (isBlackPlayer && afkTimer.name().equals("AFK Black timer"));

            if (illegalAccess)
                throw new IllegalStateException("A player cannot make a move without being at the board: " + userID);
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
                Objects.equals(gameDates, chessGame.gameDates) &&
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
        result = 31 * result + Objects.hashCode(gameDates);
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
                this.chessGameId.toString(), this.playersTurn.toString(), this.whitePlayer,
                this.blackPlayer, this.whiteRating.rating(), this.blackRating.rating(),
                this.gameDates.creationDate().toString(), this.gameDates.lastUpdateDate().toString(),
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

    private record AgreementPair(UUID whitePlayer, UUID blackPlayer) {}
}
