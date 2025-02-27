package core.project.chess.domain.chess.repositories;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.util.List;
import java.util.UUID;

public interface OutboundChessRepository {

    boolean isChessHistoryPresent(UUID chessHistoryId);

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);

    Result<List<ChessGameHistory>, Throwable> listOfGames(Username username, int limit, int offSet);

    Result<Puzzle, Throwable> puzzle(double rating);

    Result<Puzzle, Throwable> puzzle(UUID puzzleId);

    Result<List<Puzzle>, Throwable> listOfPuzzles(double rating, int limit, int offSet);
}
