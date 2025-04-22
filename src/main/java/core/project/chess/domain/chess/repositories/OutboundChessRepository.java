package core.project.chess.domain.chess.repositories;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.commons.containers.Result;

import java.util.List;
import java.util.UUID;

public interface OutboundChessRepository {

    boolean isChessHistoryPresent(UUID chessHistoryId);

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);

    Result<List<ChessGameHistory>, Throwable> listOfGames(String username, int limit, int offSet);

    Result<Puzzle, Throwable> puzzle(double minRating, double maxRating);

    Result<Puzzle, Throwable> puzzle(UUID puzzleId);

    Result<List<Puzzle>, Throwable> listOfPuzzles(double minRating, double maxRating, int limit, int offSet);
}
