package core.project.chess.domain.repositories.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;

public interface InboundUserRepository {

    UserAccount save(UserAccount userAccount);

    EmailConfirmationToken completelySave(EmailConfirmationToken emailConfirmationToken);

    void deleteUserAccountIfEmailIsNotVerified(UserAccount userAccount);

    void enableAccountAfterEmailVerification(UserAccount userAccount);
}
