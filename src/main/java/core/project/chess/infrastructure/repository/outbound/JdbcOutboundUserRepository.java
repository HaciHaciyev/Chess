package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcOutboundUserRepository implements OutboundUserRepository {

    private final JDBC jdbc;

    JdbcOutboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isEmailExists(Email verifiableEmail) {
        return false;
    }

    @Override
    public boolean isUsernameExists(Username verifiableUsername) {
        return false;
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return Optional.empty();
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        return Optional.empty();
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        return Optional.empty();
    }

    @Override
    public Optional<EmailConfirmationToken> findToken(UUID token) {
        return Optional.empty();
    }
}
