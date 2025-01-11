package core.project.chess.domain.user.repositories;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;

import java.util.UUID;

public interface OutboundUserRepository {

    boolean isEmailExists(Email verifiableEmail);

    boolean isUsernameExists(Username verifiableUsername);

    boolean havePartnership(UserAccount user, UserAccount partner);

    Result<UserAccount, Throwable> findById(UUID userId);

    Result<UserAccount, Throwable> findByUsername(Username username);

    Result<UserAccount, Throwable> findByEmail(Email email);

    Result<EmailConfirmationToken, Throwable> findToken(UUID token);

    Result<Pair<String, String>, Throwable> findRefreshToken(String refreshToken);

    Result<UserProperties, Throwable> userProperties(String username);
}
