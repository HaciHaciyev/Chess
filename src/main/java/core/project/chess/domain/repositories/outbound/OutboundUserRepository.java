package core.project.chess.domain.repositories.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.Result;

import java.util.UUID;

public interface OutboundUserRepository {

    boolean isEmailExists(Email verifiableEmail);

    boolean isUsernameExists(Username verifiableUsername);

    Result<UserAccount, Throwable> findById(UUID userId);

    Result<UserAccount, Throwable> findByUsername(Username username);

    Result<UserAccount, Throwable> findByEmail(Email email);

    Result<EmailConfirmationToken, Throwable> findToken(UUID token);
}
