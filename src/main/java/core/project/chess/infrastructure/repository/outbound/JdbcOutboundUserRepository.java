package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.*;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.exceptions.persistant.DataNotFoundException;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
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

    private static final String FIND_EMAIL = "SELECT COUNT(email) FROM UserAccount WHERE email = ?";
    private static final String FIND_USERNAME = "SELECT COUNT(username) FROM UserAccount WHERE username = ?";
    private static final String FIND_BY_ID = "SELECT * FROM UserAccount WHERE id = ?";
    private static final String FIND_BY_USERNAME = "SELECT * FROM UserAccount WHERE username = ?";
    private static final String FIND_BY_EMAIL = "SELECT * FROM UserAccount WHERE email = ?";
    private static final String FIND_REFRESH_TOKEN = "SELECT * FROM RefreshToken WHERE token = ?";
    private static final String IS_PARTNERSHIP_EXISTS = """
            SELECT * FROM UserPartnership
            WHERE (user_id = ? AND partner_id = ?) OR (user_id = ? AND partner_id = ?);
            """;
    private static final String FIND_TOKEN = """
            SELECT
            t.id AS token_id,
            t.token AS token,
            t.is_confirmed AS token_confirmation,
            t.creation_date AS token_creation_date,
            u.id AS id,
            u.username AS username,
            u.email AS email,
            u.password AS password,
            u.user_role AS user_role,
            u.rating AS rating,
            u.rating_deviation AS rating_deviation,
            u.rating_volatility AS rating_volatility,
            u.is_enable AS is_enable,
            u.creation_date AS creation_date,
            u.last_updated_date AS last_updated_date
            FROM UserToken t
            INNER JOIN UserAccount u ON t.user_id = u.id
            WHERE t.token = ?
            """;

    JdbcOutboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isEmailExists(Email verifiableEmail) {
        Result<Integer, Throwable> result = jdbc.readObjectOf(
                FIND_EMAIL,
                Integer.class,
                verifiableEmail.email()
        );

        if (!result.success()) {

            if (result.throwable() instanceof DataNotFoundException) {
                return false;
            } else {
                Log.info(result.throwable());
            }

        }

        return result.value() != null && result.value() > 0;
    }

    @Override
    public boolean isUsernameExists(Username verifiableUsername) {
        Result<Integer, Throwable> result = jdbc.readObjectOf(
                FIND_USERNAME,
                Integer.class,
                verifiableUsername.username()
        );

        if (!result.success()) {

            if (result.throwable() instanceof DataNotFoundException) {
                return false;
            } else {
                Log.info(result.throwable());
            }

        }

        return result.value() != null && result.value() > 0;
    }

    @Override
    public boolean havePartnership(UserAccount user, UserAccount partner) {
        Result<Boolean, Throwable> result = jdbc.readObjectOf(
                IS_PARTNERSHIP_EXISTS, Boolean.class, user.getId(), partner.getId(), partner.getId(), user.getId()
        );

        if (!result.success()) {
            if (result.throwable() instanceof DataNotFoundException) {
                return false;
            } else {
                Log.info(result.throwable());
            }
        }

        return result.value();
    }

    @Override
    public Result<UserAccount, Throwable> findById(UUID userId) {
        return jdbc.read(FIND_BY_ID, this::userAccountMapper, userId.toString());
    }

    @Override
    public Result<UserAccount, Throwable> findByUsername(Username username) {
        return jdbc.read(FIND_BY_USERNAME, this::userAccountMapper, username.username());
    }

    @Override
    public Result<UserAccount, Throwable> findByEmail(Email email) {
        return jdbc.read(FIND_BY_EMAIL, this::userAccountMapper, email.email());
    }

    @Override
    public Result<EmailConfirmationToken, Throwable> findToken(UUID token) {
        return jdbc.read(FIND_TOKEN, this::userTokenMapper, token.toString());
    }

    @Override
    public Result<Pair<String, String>, Throwable> findRefreshToken(String refreshToken) {
        return jdbc.read(FIND_REFRESH_TOKEN, rs -> Pair.of(rs.getString("user_id"), rs.getString("token")), refreshToken);
    }

    private EmailConfirmationToken userTokenMapper(final ResultSet rs) throws SQLException {
        var tokenEvents = new TokenEvents(rs.getObject("token_creation_date", Timestamp.class).toLocalDateTime());

        return EmailConfirmationToken.fromRepository(
                UUID.fromString(rs.getString("token_id")),
                new Token(UUID.fromString(rs.getString("token"))),
                tokenEvents,
                rs.getBoolean("token_confirmation"),
                userAccountMapper(rs)
        );
    }

    private UserAccount userAccountMapper(final ResultSet rs) throws SQLException {

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
