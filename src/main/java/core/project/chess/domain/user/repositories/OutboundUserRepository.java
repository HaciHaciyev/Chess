package core.project.chess.domain.user.repositories;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Email;

import java.util.UUID;

public interface OutboundUserRepository {

    boolean isEmailExists(String verifiableEmail);

    boolean isUsernameExists(String verifiableUsername);

    boolean havePartnership(UserAccount user, UserAccount partner);

    Result<UserAccount, Throwable> findById(UUID userId);

    Result<UserAccount, Throwable> findByUsername(String username);

    Result<UserAccount, Throwable> findByEmail(Email email);

    Result<EmailConfirmationToken, Throwable> findToken(UUID token);

    Result<Pair<String, String>, Throwable> findRefreshToken(String refreshToken);

    Result<UserProperties, Throwable> userProperties(String username);
}
