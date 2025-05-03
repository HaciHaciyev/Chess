package core.project.chess.domain.user.repositories;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.value_objects.Email;
import core.project.chess.domain.user.value_objects.RefreshToken;
import core.project.chess.domain.user.value_objects.Username;

import java.util.UUID;

public interface OutboundUserRepository {

    boolean isEmailExists(Email verifiableEmail);

    boolean isUsernameExists(Username verifiableUsername);

    boolean havePartnership(User user, User partner);

    Result<User, Throwable> findById(UUID userId);

    Result<User, Throwable> findByUsername(Username username);

    Result<User, Throwable> findByEmail(Email email);

    Result<EmailConfirmationToken, Throwable> findToken(UUID token);

    Result<RefreshToken, Throwable> findRefreshToken(String refreshToken);

    Result<UserProperties, Throwable> userProperties(Username username);
}
