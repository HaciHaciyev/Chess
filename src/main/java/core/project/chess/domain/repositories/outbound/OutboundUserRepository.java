package core.project.chess.domain.repositories.outbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Username;

import java.util.Optional;

public interface OutboundUserRepository {

    boolean isEmailExists(Email verifiableEmail);

    boolean isUsernameExists(Username verifiableUsername);

    Optional<UserAccount> findByUsername(Username username);

    Optional<UserAccount> findByEmail(Email email);
}
