package core.project.chess.domain.chess.repositories;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.chess.value_objects.PuzzleRatingWindow;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.value_objects.Username;

import java.util.List;
import java.util.UUID;

public interface OutboundChessRepository {

    boolean isChessHistoryPresent(UUID chessHistoryId);

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);

    Result<List<ChessGameHistory>, Throwable> listOfGames(Username username, int limit, int offSet);

    Result<Puzzle, Throwable> puzzle(UUID userID, PuzzleRatingWindow ratingWindow);

    Result<Puzzle, Throwable> puzzle(UUID puzzleId);

    Result<List<Puzzle>, Throwable> listOfPuzzles(double minRating, double maxRating, int limit, int offSet);
}
