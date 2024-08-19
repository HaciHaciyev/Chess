package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcOutboundChessRepository implements OutboundChessRepository {

    @Override
    public Optional<UserAccount> findById(UUID chessGameId) {
        return Optional.empty();
    }
}
