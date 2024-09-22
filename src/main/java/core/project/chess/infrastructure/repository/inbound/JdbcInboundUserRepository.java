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

    JdbcInboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(final UserAccount userAccount) {
        Log.info("Save user {%s}.".formatted(userAccount.toString()));
        final String sql = """
                    INSERT INTO UserAccount
                        (id, username, email, password, user_role,
                        rating, rating_deviation, rating_volatility,
                        is_enable, creation_date, last_updated_date)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """;

        jdbc.write(sql,
            userAccount.getId().toString(),
            userAccount.getUsername().username(),
            userAccount.getEmail().email(),
            userAccount.getPassword().password(),
            userAccount.getUserRole().toString(),
            userAccount.getRating().rating(),
            userAccount.getRating().ratingDeviation(),
            userAccount.getRating().volatility(),
            userAccount.isEnable(),
            userAccount.getAccountEvents().creationDate(),
            userAccount.getAccountEvents().lastUpdateDate()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveUserToken(final EmailConfirmationToken token) {
        Log.info("Save user token {%s}.".formatted(token.toString()));
        final String sql = """
                    INSERT INTO UserToken
                        (id, user_id, token, is_confirmed,
                        creation_date, expiration_date)
                        VALUES (?,?,?,?,?,?)
                    """;

        jdbc.write(sql,
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
        Log.info("Enable user account.");
        if (!token.isConfirmed() || !token.getUserAccount().isEnable()) {
            throw new IllegalArgumentException("Token need to be confirmed & UserAccount need to be enabled");
        }

        final String sql = """
                    UPDATE UserToken SET
                            is_confirmed = ?
                        WHERE id = ?;
                    
                    UPDATE UserAccount SET
                            is_enable = ?,
                            user_role = ?
                        WHERE id = ?;
                    """;

        jdbc.write(sql,
            token.isConfirmed(),
            token.getTokenId().toString(),
            token.getUserAccount().isEnable(),
            token.getUserAccount().getUserRole().toString(),
            token.getUserAccount().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);

        Log.info("User account {%s} has became available".formatted(token));
    }

    @Override
    public void deleteByToken(final EmailConfirmationToken token) throws IllegalAccessException {
        Log.info("Delete user account.");
        final Boolean isEnable = token.getUserAccount().isEnable();

        if (Boolean.TRUE.equals(isEnable) || token.isConfirmed()) {
            throw new IllegalAccessException("It is prohibited to delete an accessible account");
        }

        final String sql = """
                    DELETE FROM UserToken WHERE id = ?;
                    DELETE FROM UserAccount WHERE id = ?;
                    """;

        jdbc.write(sql,
            token.getTokenId().toString(),
            token.getUserAccount().getId().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }
}
