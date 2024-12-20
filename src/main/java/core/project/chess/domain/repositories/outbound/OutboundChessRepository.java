package core.project.chess.domain.repositories.outbound;

import core.project.chess.domain.aggregates.chess.value_objects.ChessGameHistory;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.util.List;
import java.util.UUID;

public interface OutboundChessRepository {

    boolean isChessHistoryPresent(UUID chessHistoryId);

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);

    Result<List<ChessGameHistory>, Throwable> listOfGames(Username username, int pageNumber);

    Result<List<String>, Throwable> listOfPartners(String name, int pageNumber);
}
