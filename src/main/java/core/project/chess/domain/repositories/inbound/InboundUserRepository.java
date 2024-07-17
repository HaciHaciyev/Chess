package core.project.chess.domain.repositories.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;

public interface InboundUserRepository {

    void save(UserAccount userAccount);

    void saveUserToken(EmailConfirmationToken token);

    void enable(EmailConfirmationToken token);

    void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException;
}
