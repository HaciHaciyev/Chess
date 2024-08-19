package core.project.chess.domain.repositories.outbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface OutboundChessRepository {

    Optional<UserAccount> findById(UUID chessGameId);
}
