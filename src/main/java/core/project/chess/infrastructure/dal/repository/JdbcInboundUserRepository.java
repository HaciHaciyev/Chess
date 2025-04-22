package core.project.chess.infrastructure.dal.repository;

import com.hadzhy.jdbclight.jdbc.JDBC;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.value_objects.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import static com.hadzhy.jdbclight.sql.SQLBuilder.*;

@Transactional
@ApplicationScoped
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JDBC jdbc;

    static final String INSERT_USER_ACCOUNT = insert()
            .into("UserAccount")
            .column("id")
            .column("firstname")
            .column("surname")
            .column("username")
            .column("email")
            .column("password")
            .column("rating")
            .column("rating_deviation")
            .column("rating_volatility")
            .column("bullet_rating")
            .column("bullet_rating_deviation")
            .column("bullet_rating_volatility")
            .column("blitz_rating")
            .column("blitz_rating_deviation")
            .column("blitz_rating_volatility")
            .column("rapid_rating")
            .column("rapid_rating_deviation")
            .column("rapid_rating_volatility")
            .column("puzzles_rating")
            .column("puzzles_rating_deviation")
            .column("puzzles_rating_volatility")
            .column("is_enable")
            .column("creation_date")
            .column("last_updated_date")
            .values()
            .build()
            .sql();

    static final String INSERT_VERIFICATION_TOKEN = insert()
            .into("UserToken")
            .column("id")
            .column("user_id")
            .column("token")
            .column("is_confirmed")
            .column("creation_date")
            .column("expiration_date")
            .values()
            .build()
            .sql();

    static final String UPDATE_USER_TOKEN_AND_ACCOUNT_VERIFICATION = batchOf(
            update("UserToken")
            .set("is_confirmed = ?")
            .where("id = ?")
            .build()
            .toSQlQuery(),
            update("UserAccount")
            .set("is_enable = ?")
            .where("id = ?")
            .build()
            .toSQlQuery());

    static final String REMOVE_VERIFICATION_TOKEN = delete()
            .from("UserToken")
            .where("user_id = ?")
            .build()
            .sql();

    static final String INSERT_OR_UPDATE_REFRESH_TOKEN = insert()
            .into("RefreshToken")
            .columns("user_id", "token")
            .values()
            .onConflict("user_id")
            .doUpdateSet("token = ?")
            .build()
            .sql();

    static final String UPDATE_USER_RATING = update("UserAccount")
            .set("""
                rating = ?,
                rating_deviation = ?,
                rating_volatility = ?
                """)
            .where("id = ?")
            .build()
            .sql();

    static final String UPDATE_USER_BULLET_RATING = update("UserAccount")
            .set("""
                 bullet_rating = ?,
                 bullet_rating_deviation = ?,
                 bullet_rating_volatility = ?
                 """)
            .where("id = ?")
            .build()
            .sql();

    static final String UPDATE_USER_BLITZ_RATING = update("UserAccount")
            .set("""
                 blitz_rating = ?,
                 blitz_rating_deviation = ?,
                 blitz_rating_volatility = ?
                 """)
            .where("id = ?")
            .build()
            .sql();

    static final String UPDATE_USER_RAPID_RATING = update("UserAccount")
            .set("""
                 rapid_rating = ?,
                 rapid_rating_deviation = ?,
                 rapid_rating_volatility = ?
                 """)
            .where("id = ?")
            .build()
            .sql();

    static final String UPDATE_USER_PUZZLES_RATING = update("UserAccount")
            .set("""
                puzzles_rating = ?,
                puzzles_rating_deviation = ?,
                puzzles_rating_volatility = ?
                """)
            .where("id = ?")
            .build()
            .sql();

    static private final String DELETE_REFRESH_TOKEN = delete()
            .from("RefreshToken")
            .where("token = ?")
            .build()
            .sql();

    JdbcInboundUserRepository() {
        this.jdbc = JDBC.instance();
    }

    @Override
    public void save(final User user) {
        jdbc.write(INSERT_USER_ACCOUNT,
                        user.id().toString(),
                        user.firstname(),
                        user.surname(),
                        user.username(),
                        user.email(),
                        user.password(),
                        user.rating().rating(),
                        user.rating().ratingDeviation(),
                        user.rating().volatility(),
                        user.bulletRating().rating(),
                        user.bulletRating().ratingDeviation(),
                        user.bulletRating().volatility(),
                        user.blitzRating().rating(),
                        user.blitzRating().ratingDeviation(),
                        user.blitzRating().volatility(),
                        user.rapidRating().rating(),
                        user.rapidRating().ratingDeviation(),
                        user.rapidRating().volatility(),
                        user.puzzlesRating().rating(),
                        user.puzzlesRating().ratingDeviation(),
                        user.puzzlesRating().volatility(),
                        user.isEnable(),
                        user.accountEvents().creationDate(),
                        user.accountEvents().lastUpdateDate())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfRating(final User user) {
        jdbc.write(UPDATE_USER_RATING,
                        user.rating().rating(),
                        user.rating().ratingDeviation(),
                        user.rating().volatility(),
                        user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfBulletRating(User user) {
        jdbc.write(UPDATE_USER_BULLET_RATING,
                        user.bulletRating().rating(),
                        user.bulletRating().ratingDeviation(),
                        user.bulletRating().volatility(),
                        user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfBlitzRating(User user) {
        jdbc.write(UPDATE_USER_BLITZ_RATING,
                        user.blitzRating().rating(),
                        user.blitzRating().ratingDeviation(),
                        user.blitzRating().volatility(),
                        user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfRapidRating(User user) {
        jdbc.write(UPDATE_USER_RAPID_RATING,
                        user.rapidRating().rating(),
                        user.rapidRating().ratingDeviation(),
                        user.rapidRating().volatility(),
                        user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfPuzzleRating(final User user) {
        Rating puzzlesRating = user.puzzlesRating();
        jdbc.write(UPDATE_USER_PUZZLES_RATING,
                        puzzlesRating.rating(),
                        puzzlesRating.ratingDeviation(),
                        puzzlesRating.volatility(),
                        user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveVerificationToken(final EmailConfirmationToken token) {
        jdbc.write(INSERT_VERIFICATION_TOKEN,
                        token.tokenID().toString(),
                        token.user().id().toString(),
                        token.token().token().toString(),
                        Boolean.FALSE,
                        token.tokenEvents().creationDate(),
                        token.tokenEvents().expirationDate())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void removeVerificationToken(User user) {
        jdbc.write(REMOVE_VERIFICATION_TOKEN, user.id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateUserVerification(final EmailConfirmationToken token) {
        if (!token.isConfirmed() || !token.user().isEnable())
            throw new IllegalArgumentException("Token needs to be confirmed & UserAccount needs to be enabled");

        jdbc.write(UPDATE_USER_TOKEN_AND_ACCOUNT_VERIFICATION,
                        token.isConfirmed(),
                        token.tokenID().toString(),
                        token.user().isEnable(),
                        token.user().id().toString())
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveRefreshToken(User user, String refreshToken) {
        jdbc.write(INSERT_OR_UPDATE_REFRESH_TOKEN, user.id().toString(), refreshToken, refreshToken)
                .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void removeRefreshToken(String refreshToken) {
        jdbc.write(DELETE_REFRESH_TOKEN, refreshToken).ifFailure(Throwable::printStackTrace);
    }
}
