package core.project.chess.domain.repositories.outbound;

import core.project.chess.application.dto.gamesession.ChessGameHistory;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.util.UUID;

public interface OutboundChessRepository {

    boolean isChessHistoryPresent(UUID chessHistoryId);

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);
}
