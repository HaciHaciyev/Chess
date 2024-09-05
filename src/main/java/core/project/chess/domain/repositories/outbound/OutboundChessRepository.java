package core.project.chess.domain.repositories.outbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.infrastructure.utilities.Result;

import java.util.UUID;

public interface OutboundChessRepository {

    Result<ChessGame, Throwable> findById(UUID chessGameId);
}
