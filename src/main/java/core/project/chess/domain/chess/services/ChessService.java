package core.project.chess.domain.chess.services;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.AgreementResult;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.UndoMoveResult;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.value_objects.*;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.value_objects.GameResult;
import core.project.chess.domain.commons.value_objects.Ratings;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ChessService {

    private final InboundChessRepository inboundChessRepository;

    private final OutboundChessRepository outboundChessRepository;

    ChessService(InboundChessRepository inboundChessRepository,
                 OutboundChessRepository outboundChessRepository) {
        this.inboundChessRepository = inboundChessRepository;
        this.outboundChessRepository = outboundChessRepository;
    }

    public boolean validateOpponentEligibility(
            final Pair<UUID, Ratings> player,
            final GameParameters gameParameters,
            final Pair<UUID, Ratings> opponent,
            final GameParameters opponentGameParameters,
            final boolean isPartnershipGame) {

        final boolean sameUser = player.getFirst().equals(opponent.getFirst());
        if (sameUser) return false;

        final boolean sameTimeControlling = gameParameters.time().equals(opponentGameParameters.time());
        if (!sameTimeControlling) return false;

        if (!isPartnershipGame) {
            final boolean validRatingDiff = isValidRatingDifferance(player.getSecond(), opponent.getSecond(), gameParameters.time());
            if (!validRatingDiff) return false;
        }

        if (isPartnershipGame && !(gameParameters.isCasualGame().equals(opponentGameParameters.isCasualGame()))) return false;

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) return true;

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }

    private static boolean isValidRatingDifferance(Ratings player, Ratings opponent, ChessGame.Time time) {
        return switch (time) {
            case CLASSIC, DEFAULT -> ratingDiff(player.rating().rating(), opponent.rating().rating()) <= 1500;
            case RAPID -> ratingDiff(player.rapidRating().rating(), opponent.rapidRating().rating()) <= 1500;
            case BULLET -> ratingDiff(player.bulletRating().rating(), opponent.bulletRating().rating()) <= 1500;
            case BLITZ -> ratingDiff(player.blitzRating().rating(), opponent.blitzRating().rating()) <= 1500;
        };
    }

    private static double ratingDiff(double rating, double rating1) {
        return Math.abs(rating1 - rating);
    }

    public Result<GameStateUpdate, Throwable> move(UUID userID, ChessGame chessGame,
                                                   Coordinate from, Coordinate to, @Nullable String promotion) {
        try {
            chessGame.doMove(userID, from, to, getPromotion(promotion));
            return Result.success(new GameStateUpdate(
                    chessGame.chessGameID(),
                    chessGame.fen(),
                    chessGame.pgn(),
                    remainingTimeAsString(chessGame),
                    chessGame.isThreeFoldActive()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e);
        }
    }

    public String remainingTimeAsString(ChessGame cg) {
        if (cg.countOfHalfMoves() == 0 || cg.countOfHalfMoves() == 1) {
            return "W -> 02:59:59 | B -> 03:00:00";
        }

        Duration whiteRemaining = cg.remainingTimeForWhite();

        long wHH = whiteRemaining.toHours();
        int wMM = whiteRemaining.toMinutesPart();
        int wSS = whiteRemaining.toSecondsPart();

        String wTime = "W -> %02d:%02d:%02d".formatted(wHH, wMM, wSS);

        Duration blackRemaining = cg.remainingTimeForBlack();

        long bHH = blackRemaining.toHours();
        int bMM = blackRemaining.toMinutesPart();
        int bSS = blackRemaining.toSecondsPart();

        String bTime = "B -> %02d:%02d:%02d".formatted(bHH, bMM, bSS);

        return wTime + " | " + bTime;
    }

    public Result<ChatMessage, Throwable> chat(String message, UUID userID, ChessGame chessGame) {
        try {
            ChatMessage chatMessage = new ChatMessage(message);
            chessGame.addChatMessage(userID, chatMessage);
            return Result.success(chatMessage);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Result.failure(e);
        }
    }

    public UndoMoveResult returnOfMovement(UUID userID, ChessGame chessGame) {
        try {
            return chessGame.undo(userID);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return UndoMoveResult.FAILED_UNDO;
        }
    }

    public Result<GameResult, Throwable> resignation(UUID userID, ChessGame chessGame) {
        try {
            chessGame.resignation(userID);
            return Result.success(chessGame.gameResult());
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }
    }

    public boolean threeFold(UUID userID, ChessGame chessGame) {
        try {
            chessGame.endGameByThreeFold(userID);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    public AgreementResult agreement(UUID userID, ChessGame chessGame) {
        try {
            chessGame.agreement(userID);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return AgreementResult.FAILED;
        }

        if (chessGame.isAgreementAvailable()) return AgreementResult.REQUESTED;
        return AgreementResult.AGREED;
    }

    public Result<PuzzleStateUpdate, Throwable> puzzleMove(Puzzle puzzle, Coordinate from,
                                                           Coordinate to, @Nullable String promotion) {
        try {
            Piece inCaseOfPromotion = getPromotion(promotion);
            puzzle.makeMovement(from, to, inCaseOfPromotion);
            return Result.success(new PuzzleStateUpdate(
                    puzzle.id(),
                    puzzle.pgn(),
                    puzzle.fen(),
                    puzzle.isEnded(),
                    puzzle.isSolved()
            ));
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }
    }

    private static Piece getPromotion(String promotion) {
        return Objects.isNull(promotion) ? null : AlgebraicNotation.fromSymbol(promotion);
    }

    public void executeGameOverOperations(final ChessGame chessGame) {
        if (chessGame.gameResult() == GameResult.NONE) throw new IllegalStateException("You can`t save not finished game.");
        if (outboundChessRepository.isChessHistoryPresent(chessGame.historyID())) {
            Log.infof("History of game %s is already present", chessGame.chessGameID());
            return;
        }

        inboundChessRepository.completelyUpdateFinishedGame(chessGame);
    }
}
