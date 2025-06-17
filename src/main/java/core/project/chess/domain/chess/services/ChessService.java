package core.project.chess.domain.chess.services;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.AgreementResult;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResult;
import core.project.chess.domain.chess.enumerations.UndoMoveResult;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.value_objects.*;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Objects;

@ApplicationScoped
public class ChessService {

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundChessRepository outboundChessRepository;

    ChessService(InboundUserRepository inboundUserRepository,
                 InboundChessRepository inboundChessRepository,
                 OutboundChessRepository outboundChessRepository) {
        this.inboundUserRepository = inboundUserRepository;
        this.inboundChessRepository = inboundChessRepository;
        this.outboundChessRepository = outboundChessRepository;
    }

    public boolean validateOpponentEligibility(
            final User player,
            final GameParameters gameParameters,
            final User opponent,
            final GameParameters opponentGameParameters,
            final boolean isPartnershipGame) {

        final boolean sameUser = player.id().equals(opponent.id());
        if (sameUser) return false;

        final boolean sameTimeControlling = gameParameters.time().equals(opponentGameParameters.time());
        if (!sameTimeControlling) return false;

        if (!isPartnershipGame) {
            final boolean validRatingDiff = Math.abs(player.rating().rating() - opponent.rating().rating()) <= 1500;
            if (!validRatingDiff) return false;
        }

        if (isPartnershipGame && !(gameParameters.isCasualGame().equals(opponentGameParameters.isCasualGame()))) return false;

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) return true;

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }

    public Result<GameStateUpdate, Throwable> move(String username, ChessGame chessGame,
                                                   Coordinate from, Coordinate to, @Nullable String promotion) {
        try {
            chessGame.doMove(username, from, to, getPromotion(promotion));
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

    public Result<ChatMessage, Throwable> chat(String message, String username, ChessGame chessGame) {
        try {
            ChatMessage chatMessage = new ChatMessage(message);
            chessGame.addChatMessage(username, chatMessage);
            return Result.success(chatMessage);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Result.failure(e);
        }
    }

    public UndoMoveResult returnOfMovement(String username, ChessGame chessGame) {
        try {
            return chessGame.undo(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return UndoMoveResult.FAILED_UNDO;
        }
    }

    public Result<GameResult, Throwable> resignation(String username, ChessGame chessGame) {
        try {
            chessGame.resignation(username);
            return Result.success(chessGame.gameResult());
        } catch (IllegalArgumentException e) {
            return Result.failure(e);
        }
    }

    public boolean threeFold(String username, ChessGame chessGame) {
        try {
            chessGame.endGameByThreeFold(username);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    public AgreementResult agreement(String username, ChessGame chessGame) {
        try {
            chessGame.agreement(username);
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

        Log.infof("Saving finished game %s and changing ratings", chessGame.chessGameID());
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);

        if (chessGame.isCasualGame()) return;
        switch (chessGame.time()) {
            case CLASSIC, DEFAULT -> {
                inboundUserRepository.updateOfRating(chessGame.whitePlayer());
                inboundUserRepository.updateOfRating(chessGame.blackPlayer());
            }
            case BULLET -> {
                inboundUserRepository.updateOfBulletRating(chessGame.whitePlayer());
                inboundUserRepository.updateOfBulletRating(chessGame.blackPlayer());
            }
            case BLITZ -> {
                inboundUserRepository.updateOfBlitzRating(chessGame.whitePlayer());
                inboundUserRepository.updateOfBlitzRating(chessGame.blackPlayer());
            }
            case RAPID -> {
                inboundUserRepository.updateOfRapidRating(chessGame.whitePlayer());
                inboundUserRepository.updateOfRapidRating(chessGame.blackPlayer());
            }
        }
    }
}
