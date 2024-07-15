package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcOutboundUserRepository implements OutboundUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isEmailExists(Email email) {
        return false;
    }

    @Override
    public boolean isUsernameExists(Username username) {
        return false;
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        return Optional.empty();
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        return Optional.empty();
    }
}
