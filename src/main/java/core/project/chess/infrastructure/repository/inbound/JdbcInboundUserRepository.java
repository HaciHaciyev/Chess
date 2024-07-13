package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserAccount save(UserAccount userAccount) {
        return null;
    }
}
