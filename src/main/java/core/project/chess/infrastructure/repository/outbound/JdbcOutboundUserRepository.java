package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.*;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.exceptions.DataNotFoundException;
import core.project.chess.infrastructure.exceptions.RepositoryDataException;
import core.project.chess.infrastructure.utilities.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

@Transactional
@ApplicationScoped
public class JdbcOutboundUserRepository implements OutboundUserRepository {

    private final JDBC jdbc;

    JdbcOutboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isEmailExists(Email verifiableEmail) {
        String findEmail = "Select COUNT(email) from UserAccount where email = ?";

        Integer count = jdbc.queryForObject(
                findEmail,
                Integer.class,
                verifiableEmail.email()
        )

        .mapFailure(throwable -> {
            if (throwable instanceof DataNotFoundException) {
                return -1;
            }

            throw new RepositoryDataException(throwable.getMessage());
        }).get();

        return count != null && count > 0;
    }

    @Override
    public boolean isUsernameExists(Username verifiableUsername) {
        String findEmail = "Select COUNT(username) from UserAccount where username = ?";

        Integer count = jdbc.queryForObject(
                findEmail,
                Integer.class,
                verifiableUsername.username()
        )

        .mapFailure(throwable -> {
            if (throwable instanceof DataNotFoundException) {
                return -1;
            }

            throw new RepositoryDataException(throwable.getMessage());
        }).get();

        return count != null && count > 0;
    }

    @Override
    public Result<UserAccount, Throwable> findById(UUID userId) {
        String selectByUsername = "Select * from UserAccount where id = ?";

        return jdbc.query(selectByUsername, this::userAccountMapper, userId.toString());
    }

    @Override
    public Result<UserAccount, Throwable> findByUsername(Username username) {
        String selectByUsername = "Select * from UserAccount where username = ?";

        return jdbc.query(selectByUsername, this::userAccountMapper, username.username());
    }

    @Override
    public Result<UserAccount, Throwable> findByEmail(Email email) {
        String selectByUsername = "Select * from UserAccount where email = ?";

        return jdbc.query(selectByUsername, this::userAccountMapper, email.email());
    }

    @Override
    public Result<EmailConfirmationToken, Throwable> findToken(UUID token) {
        String selectUserToken =
                """
                SELECT
                t.id AS token_id,
                t.token AS token,
                t.is_confirmed AS token_confirmation,
                t.creation_date AS token_creation_date,
                
                u.id AS id,
                u.username AS username,
                u.email AS email,
                u.password AS password,
                u.rating AS rating,
                u.is_enable AS is_enable,
                u.creation_date AS creation_date,
                u.last_updated_date AS last_updated_date
                FROM UserToken t
                INNER JOIN UserAccount u ON t.user_id = u.id
                WHERE t.token = ?
                """;

        return jdbc.query(selectUserToken, this::userTokenMapper, token.toString());
    }

    private EmailConfirmationToken userTokenMapper(final ResultSet rs) throws SQLException {
        var tokenEvents = new TokenEvents(rs.getObject("token_creation_date", Timestamp.class).toLocalDateTime());

        return EmailConfirmationToken.fromRepository(
                UUID.fromString("token_id"), new Token(UUID.fromString(rs.getString("token"))), tokenEvents,
                rs.getBoolean("token_confirmation"), userAccountMapper(rs)
        );
    }

    private UserAccount userAccountMapper(final ResultSet rs) throws SQLException {
        Log.info("The user account {%s} was taken from the database".formatted(rs.getString("username")));

        var events = new AccountEvents(
                rs.getObject("creation_date", Timestamp.class).toLocalDateTime(),
                rs.getObject("last_updated_date", Timestamp.class).toLocalDateTime()
        );

        var rating = Rating.fromRepository(
                rs.getDouble("rating"),
                rs.getDouble("rating_deviation"),
                rs.getDouble("rating_volatility")
        );

        return UserAccount.fromRepository(
                UUID.fromString(rs.getString("id")),
                new Username(rs.getString("username")),
                new Email(rs.getString("email")),
                new Password(rs.getString("password")),
                UserRole.valueOf(rs.getString("user_role")),
                rs.getBoolean("is_enable"),
                rating,
                events
        );
    }
}
