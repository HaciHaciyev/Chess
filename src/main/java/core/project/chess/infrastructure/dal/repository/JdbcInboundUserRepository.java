package core.project.chess.infrastructure.dal.repository;

import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.value_objects.Rating;
import core.project.chess.infrastructure.dal.util.jdbc.JDBC;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.*;

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
            .values(25)
            .build();

    static final String INSERT_NEW_PARTNERSHIP = insert()
            .into("UserPartnership")
            .columns("user_id", "partner_id")
            .values(2)
            .build();

    static final String INSERT_USER_TOKEN = insert()
            .into("UserToken")
            .column("id")
            .column("user_id")
            .column("token")
            .column("is_confirmed")
            .column("creation_date")
            .column("expiration_date")
            .values(6)
            .build();

    static final String INSERT_OR_UPDATE_REFRESH_TOKEN = insert()
            .into("RefreshToken")
            .columns("user_id", "token")
            .values(2)
            .onConflict("user_id")
            .doUpdateSet("token = ?")
            .build();

    static final String UPDATE_USER_RATING = update("UserAccount")
            .set("""
                rating = ?,
                rating_deviation = ?,
                rating_volatility = ?
                """)
            .where("id = ?")
            .build();

    static final String UPDATE_USER_BULLET_RATING = update("UserAccount")
            .set("""
                 bullet_rating = ?,
                 bullet_rating_deviation = ?,
                 bullet_rating_volatility = ?
                 """)
            .where("id = ?")
            .build();

    static final String UPDATE_USER_BLITZ_RATING = update("UserAccount")
            .set("""
                 blitz_rating = ?,
                 blitz_rating_deviation = ?,
                 blitz_rating_volatility = ?
                 """)
            .where("id = ?")
            .build();

    static final String UPDATE_USER_RAPID_RATING = update("UserAccount")
            .set("""
                 rapid_rating = ?,
                 rapid_rating_deviation = ?,
                 rapid_rating_volatility = ?
                 """)
            .where("id = ?")
            .build();

    static final String UPDATE_USER_PUZZLES_RATING = update("UserAccount")
            .set("""
                puzzles_rating = ?,
                puzzles_rating_deviation = ?,
                puzzles_rating_volatility = ?
                """)
            .where("id = ?")
            .build();

    static final String UPDATE_USER_TOKEN_AND_ACCOUNT = batchOf(
            update("UserToken")
            .set("is_confirmed = ?")
            .where("id = ?")
            .build(),
            update("UserAccount")
            .set("is_enable = ?, user_role = ?")
            .where("id = ?")
            .build());

    static final String DELETE_USER_TOKEN_AND_ACCOUNT = batchOf(
            delete()
            .from("UserToken")
            .where("id = ?")
            .build(),
            delete()
            .from("UserAccount")
            .where("id = ?")
            .build());

    static private final String DELETE_REFRESH_TOKEN = delete()
            .from("RefreshToken")
            .where("token = ?")
            .build();

    JdbcInboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
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
            user.isEnabled(),
            user.accountEvents().creationDate(),
            user.accountEvents().lastUpdateDate()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfRating(final User user) {

        jdbc.write(UPDATE_USER_RATING,
                user.rating().rating(),
                user.rating().ratingDeviation(),
                user.rating().volatility(),
                user.id().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfBulletRating(User user) {

        jdbc.write(UPDATE_USER_BULLET_RATING,
                user.bulletRating().rating(),
                user.bulletRating().ratingDeviation(),
                user.bulletRating().volatility(),
                user.id().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfBlitzRating(User user) {

        jdbc.write(UPDATE_USER_BLITZ_RATING,
                user.blitzRating().rating(),
                user.blitzRating().ratingDeviation(),
                user.blitzRating().volatility(),
                user.id().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfRapidRating(User user) {

        jdbc.write(UPDATE_USER_RAPID_RATING,
                user.rapidRating().rating(),
                user.rapidRating().ratingDeviation(),
                user.rapidRating().volatility(),
                user.id().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void updateOfPuzzleRating(final User user) {
        Rating puzzlesRating = user.puzzlesRating();
        jdbc.write(UPDATE_USER_PUZZLES_RATING,
                puzzlesRating.rating(),
                puzzlesRating.ratingDeviation(),
                puzzlesRating.volatility(),
                user.id().toString()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveUserToken(final EmailConfirmationToken token) {

        jdbc.write(INSERT_USER_TOKEN,
            token.tokenID().toString(),
            token.user().id().toString(),
            token.token().token().toString(),
            Boolean.FALSE,
            token.tokenEvents().creationDate(),
            token.tokenEvents().expirationDate()
        )

        .ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void enable(final EmailConfirmationToken token) {
        if (!token.isConfirmed() || !token.user().isEnabled()) {
            throw new IllegalArgumentException("Token needs to be confirmed & UserAccount needs to be enabled");
        }

        jdbc.write(UPDATE_USER_TOKEN_AND_ACCOUNT,
            token.isConfirmed(),
            token.tokenID().toString(),
            token.user().isEnabled(),
            token.user().id().toString()
        )

        .ifFailure(Throwable::printStackTrace);

    }

    @Override
    public void deleteByToken(final EmailConfirmationToken token) throws IllegalAccessException {
        final boolean isEnable = token.user().isEnabled();

        if (isEnable || token.isConfirmed()) {
            throw new IllegalAccessException("It is prohibited to delete an accessible account");
        }

        jdbc.write(DELETE_USER_TOKEN_AND_ACCOUNT,
            token.tokenID().toString(),
            token.user().id().toString()
        )

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
