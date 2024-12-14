package core.project.chess.domain.repositories.inbound;

import core.project.chess.domain.subdomains.user.entities.EmailConfirmationToken;
import core.project.chess.domain.subdomains.user.entities.UserAccount;

public interface InboundUserRepository {

    void save(UserAccount userAccount);

    void updateOfRating(UserAccount userAccount);

    void saveUserToken(EmailConfirmationToken token);

    void enable(EmailConfirmationToken token);

    void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException;

    void addPartnership(UserAccount firstUser, UserAccount secondUser);

    void saveRefreshToken(UserAccount userAccount, String refreshToken);
}
