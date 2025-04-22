package core.project.chess.domain.user.repositories;

import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;

public interface InboundUserRepository {

    void save(User user);

    void updateOfRating(User user);

    void updateOfBulletRating(User user);

    void updateOfBlitzRating(User user);

    void updateOfRapidRating(User user);

    void updateOfPuzzleRating(User user);

    void saveVerificationToken(EmailConfirmationToken token);

    void removeVerificationToken(User user);

    void updateUserVerification(EmailConfirmationToken token);

    void saveRefreshToken(User user, String refreshToken);

    void removeRefreshToken(String refreshToken);
}
