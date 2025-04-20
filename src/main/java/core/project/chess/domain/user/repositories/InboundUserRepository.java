package core.project.chess.domain.user.repositories;

import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;

public interface InboundUserRepository {

    void save(UserAccount userAccount);

    void updateOfRating(UserAccount userAccount);

    void updateOfBulletRating(UserAccount userAccount);

    void updateOfBlitzRating(UserAccount userAccount);

    void updateOfRapidRating(UserAccount userAccount);

    void updateOfPuzzleRating(UserAccount userAccount);

    void saveUserToken(EmailConfirmationToken token);

    void enable(EmailConfirmationToken token);

    void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException;

    void saveRefreshToken(UserAccount userAccount, String refreshToken);

    void removeRefreshToken(String refreshToken);
}
