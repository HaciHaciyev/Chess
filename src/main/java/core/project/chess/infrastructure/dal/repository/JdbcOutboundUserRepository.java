package core.project.chess.infrastructure.dal.repository;

import com.hadzhy.jdbclight.jdbc.JDBC;
import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.events.AccountEvents;
import core.project.chess.domain.user.events.TokenEvents;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import static com.hadzhy.jdbclight.sql.SQLBuilder.select;

@Transactional
@ApplicationScoped
public class JdbcOutboundUserRepository implements OutboundUserRepository {

    private final JDBC jdbc;

    static final String FIND_BY_ID = select()
            .all()
            .from("UserAccount")
            .where("id = ?")
            .build()
            .sql();

    static final String FIND_BY_USERNAME = select()
            .all()
            .from("UserAccount")
            .where("username = ?")
            .build()
            .sql();

    static final String FIND_BY_EMAIL = select()
            .all()
            .from("UserAccount")
            .where("email = ?")
            .build()
            .sql();

    static final String FIND_REFRESH_TOKEN = select()
            .all()
            .from("RefreshToken")
            .where("token = ?")
            .build()
            .sql();

    static final String FIND_EMAIL = select()
            .count("*")
            .from("UserAccount")
            .where("email = ?")
            .build()
            .sql();

    static final String FIND_USERNAME = select()
            .count("*")
            .from("UserAccount")
            .where("username = ?")
            .build()
            .sql();

    static final String FIND_USER_PROPERTIES = select()
            .column("firstname")
            .column("surname")
            .column("username")
            .column("email")
            .column("rating")
            .column("bullet_rating")
            .column("blitz_rating")
            .column("rapid_rating")
            .column("puzzles_rating")
            .from("UserAccount")
            .where("username = ?")
            .build()
            .sql();

    static final String IS_PARTNERSHIP_EXISTS = select()
            .count("*")
            .from("UserPartnership")
            .where("(user_id = ? AND partner_id = ?)")
            .or("(user_id = ? AND partner_id = ?)")
            .build()
            .sql();

    static final String FIND_TOKEN = select()
            .column("t.id").as("token_id")
            .column("t.token").as("token")
            .column("t.is_confirmed").as("token_confirmation")
            .column("t.creation_date").as("token_creation_date")
            .column("u.id").as("id")
            .column("u.firstname").as("firstname")
            .column("u.surname").as("surname")
            .column("u.username").as("username")
            .column("u.email").as("email")
            .column("u.password").as("password")
            .column("u.rating").as("rating")
            .column("u.rating_deviation").as("rating_deviation")
            .column("u.rating_volatility").as("rating_volatility")
            .column("u.bullet_rating").as("bullet_rating")
            .column("u.bullet_rating_deviation").as("bullet_rating_deviation")
            .column("u.bullet_rating_volatility").as("bullet_rating_volatility")
            .column("u.blitz_rating").as("blitz_rating")
            .column("u.blitz_rating_deviation").as("blitz_rating_deviation")
            .column("u.blitz_rating_volatility").as("blitz_rating_volatility")
            .column("u.rapid_rating").as("rapid_rating")
            .column("u.rapid_rating_deviation").as("rapid_rating_deviation")
            .column("u.rapid_rating_volatility").as("rapid_rating_volatility")
            .column("u.puzzles_rating").as("puzzles_rating")
            .column("u.puzzles_rating_deviation").as("puzzles_rating_deviation")
            .column("u.puzzles_rating_volatility").as("puzzles_rating_volatility")
            .column("u.is_enable").as("is_enable")
            .column("u.creation_date").as("creation_date")
            .column("u.last_updated_date").as("last_updated_date")
            .from("UserToken t")
            .innerJoin("UserAccount u", "t.user_id = u.id")
            .where("t.token = ?")
            .build()
            .sql();

    JdbcOutboundUserRepository() {
        this.jdbc = JDBC.instance();
    }

    @Override
    @WithSpan("Email existence check")
    public boolean isEmailExists(Email verifiableEmail) {
        return jdbc.readObjectOf(FIND_EMAIL, Integer.class, verifiableEmail.email())
                .mapSuccess(count -> count != null && count > 0)
                .orElseGet(() -> {
                    Log.error("Error checking email existence.");
                    return false;
                });
    }

    @Override
    @WithSpan("Username existence check")
    public boolean isUsernameExists(Username verifiableUsername) {
        return jdbc.readObjectOf(FIND_USERNAME, Integer.class, verifiableUsername.username())
                .mapSuccess(count -> count != null && count > 0)
                .orElseGet(() -> {
                    Log.error("Error checking username existence.");
                    return false;
                });
    }

    @Override
    public boolean havePartnership(User user, User partner) {
        return jdbc.readObjectOf(IS_PARTNERSHIP_EXISTS,
                        Integer.class,
                        user.id().toString(),
                        partner.id().toString(),
                        partner.id().toString(),
                        user.id().toString())
                .mapSuccess(count -> count != null && count > 0)
                .orElseGet(() -> {
                    Log.error("Error checking partnership existence.");
                    return false;
                });
    }

    @Override
    public Result<User, Throwable> findById(UUID userId) {
        var user = jdbc.read(FIND_BY_ID, this::userAccountMapper, userId.toString());
        return new Result<>(user.value(), user.throwable(), user.success());
    }

    @Override
    public Result<User, Throwable> findByUsername(Username username) {
        var user = jdbc.read(FIND_BY_USERNAME, this::userAccountMapper, username.username());
        return new Result<>(user.value(), user.throwable(), user.success());
    }

    @Override
    public Result<User, Throwable> findByEmail(Email email) {
        var user = jdbc.read(FIND_BY_EMAIL, this::userAccountMapper, email.email());
        return new Result<>(user.value(), user.throwable(), user.success());
    }

    @Override
    public Result<EmailConfirmationToken, Throwable> findToken(UUID token) {
        var result = jdbc.read(FIND_TOKEN, this::userTokenMapper, token.toString());
        return new Result<>(result.value(), result.throwable(), result.success());
    }

    @Override
    public Result<RefreshToken, Throwable> findRefreshToken(String refreshToken) {
        var result = jdbc.read(FIND_REFRESH_TOKEN, this::refreshTokenMapper, refreshToken);
        return new Result<>(result.value(), result.throwable(), result.success());
    }

    @Override
    public Result<UserProperties, Throwable> userProperties(Username username) {
        var result = jdbc.read(FIND_USER_PROPERTIES, this::userPropertiesMapper, username.username());
        return new Result<>(result.value(), result.throwable(), result.success());
    }

    private RefreshToken refreshTokenMapper(final ResultSet rs) throws SQLException {
        return new RefreshToken(UUID.fromString(rs.getString("user_id")), rs.getString("token"));
    }

    private UserProperties userPropertiesMapper(final ResultSet rs) throws SQLException {
        return new UserProperties(
                rs.getString("firstname"),
                rs.getString("surname"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getDouble("rating"),
                rs.getDouble("bullet_rating"),
                rs.getDouble("blitz_rating"),
                rs.getDouble("rapid_rating"),
                rs.getDouble("puzzles_rating")
        );
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

    private User userAccountMapper(final ResultSet rs) throws SQLException {
        var events = new AccountEvents(
                rs.getObject("creation_date", Timestamp.class).toLocalDateTime(),
                rs.getObject("last_updated_date", Timestamp.class).toLocalDateTime()
        );

        var rating = Rating.fromRepository(
                rs.getDouble("rating"),
                rs.getDouble("rating_deviation"),
                rs.getDouble("rating_volatility")
        );

        var bulletRating = Rating.fromRepository(
                rs.getDouble("bullet_rating"),
                rs.getDouble("bullet_rating_deviation"),
                rs.getDouble("bullet_rating_volatility")
        );

        var blitzRating = Rating.fromRepository(
                rs.getDouble("blitz_rating"),
                rs.getDouble("blitz_rating_deviation"),
                rs.getDouble("blitz_rating_volatility")
        );

        var rapidRating = Rating.fromRepository(
                rs.getDouble("rapid_rating"),
                rs.getDouble("rapid_rating_deviation"),
                rs.getDouble("rapid_rating_volatility")
        );

        var puzzlesRating = Rating.fromRepository(
                rs.getDouble("puzzles_rating"),
                rs.getDouble("puzzles_rating_deviation"),
                rs.getDouble("puzzles_rating_volatility")
        );

        return User.fromRepository(
                UUID.fromString(rs.getString("id")),
                new PersonalData(
                        rs.getString("firstname"),
                        rs.getString("surname"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password")
                ),
                rs.getBoolean("is_enable"),
                new Ratings(rating,
                        bulletRating,
                        blitzRating,
                        rapidRating,
                        puzzlesRating
                ),
                events
        );
    }
}
