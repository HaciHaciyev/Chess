package core.project.chess.domain.repositories.outbound;

import core.project.chess.application.model.ChessGameHistory;
import core.project.chess.infrastructure.utilities.Result;

import java.util.UUID;

public interface OutboundChessRepository {

    Result<ChessGameHistory, Throwable> findById(UUID chessGameId);
}
