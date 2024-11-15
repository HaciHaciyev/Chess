package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Transactional
@ApplicationScoped
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JDBC jdbc;

    private static final String INSERT_USER_ACCOUNT = """
            INSERT INTO UserAccount
                (id, username, email, password, user_role,
                rating, rating_deviation, rating_volatility,
                is_enable, creation_date, last_updated_date)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_NEW_PARTNERSHIP = """
            INSERT INTO UserPartnership
                (user_id, partner_id)
                VALUES (?,?)
            """;

    private static final String INSERT_USER_TOKEN = """
            INSERT INTO UserToken
                (id, user_id, token, is_confirmed,
                creation_date, expiration_date)
                VALUES (?,?,?,?,?,?)
            """;

    private static final String UPDATE_USER_RATING = """
            UPDATE UserAccount SET
                    rating = ?,
                    rating_deviation = ?,
                    rating_volatility = ?
                WHERE id = ?
            """;

    private static final String UPDATE_USER_TOKEN_AND_ACCOUNT = """
            UPDATE UserToken SET
                    is_confirmed = ?
                WHERE id = ?;

            UPDATE UserAccount SET
                    is_enable = ?,
                    user_role = ?
                WHERE id = ?;
            """;

    private static final String DELETE_USER_TOKEN_AND_ACCOUNT = """
            DELETE FROM UserToken WHERE id = ?;
            DELETE FROM UserAccount WHERE id = ?;
            """;

    private static final String INSERT_OR_UPDATE_REFRESH_TOKEN = """
            INSERT INTO RefreshToken (user_id, token) VALUES (?, ?) ON CONFLICT (user_id) DO UPDATE SET token = ?;
            """;

    JdbcInboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(final UserAccount userAccount) {

        jdbc.write(INSERT_USER_ACCOUNT,
            userAccount.getId().toString(),
            userAccount.getUsername().username(),
            userAccount.getEmail().email(),
            userAccount.getPassword().password(),
            userAccount.getUserRole().toString(),
            userAccount.getRating().rating(),
            userAccount.getRating().ratingDeviation(),
            userAccount.getRating().volatility(),
            userAccount.isEnabled(),
            userAccount.getAccountEvents().creationDate(),
            userAccount.getAccountEvents().lastUpdateDate()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfRating(final UserAccount userAccount) {

        jdbc.write(UPDATE_USER_RATING,
                userAccount.getRating().rating(),
                userAccount.getRating().ratingDeviation(),
                userAccount.getRating().volatility(),
                userAccount.getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveUserToken(final EmailConfirmationToken token) {

        jdbc.write(INSERT_USER_TOKEN,
            token.getTokenId().toString(),
            token.getUserAccount().getId().toString(),
            token.getToken().token().toString(),
            Boolean.FALSE,
            token.getTokenEvents().getCreationDate(),
            token.getTokenEvents().getExpirationDate()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void enable(final EmailConfirmationToken token) {
        if (!token.isConfirmed() || !token.getUserAccount().isEnabled()) {
            throw new IllegalArgumentException("Token need to be confirmed & UserAccount need to be enabled");
        }

        jdbc.write(UPDATE_USER_TOKEN_AND_ACCOUNT,
            token.isConfirmed(),
            token.getTokenId().toString(),
            token.getUserAccount().isEnabled(),
            token.getUserAccount().getUserRole().toString(),
            token.getUserAccount().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);

    }

    @Override
    public void deleteByToken(final EmailConfirmationToken token) throws IllegalAccessException {
        final boolean isEnable = token.getUserAccount().isEnabled();

        if (isEnable || token.isConfirmed()) {
            throw new IllegalAccessException("It is prohibited to delete an accessible account");
        }

        jdbc.write(DELETE_USER_TOKEN_AND_ACCOUNT,
            token.getTokenId().toString(),
            token.getUserAccount().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void addPartnership(UserAccount firstUser, UserAccount secondUser) {
        final boolean doNotMatch = !firstUser.getPartners().contains(secondUser) || !secondUser.getPartners().contains(firstUser);
        if (doNotMatch) {
            throw new IllegalArgumentException("Illegal function usage.");
        }

        jdbc.write(INSERT_NEW_PARTNERSHIP,
                firstUser.getId().toString(),
                secondUser.getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveRefreshToken(UserAccount userAccount, String refreshToken) {
        jdbc.write(INSERT_OR_UPDATE_REFRESH_TOKEN, userAccount.getId().toString(), refreshToken, refreshToken).ifFailure(Throwable::printStackTrace);
    }
}
