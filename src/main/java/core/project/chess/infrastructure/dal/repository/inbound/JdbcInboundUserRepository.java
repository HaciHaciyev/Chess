package core.project.chess.infrastructure.dal.repository.inbound;

import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;
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
            .columns("id",
                    "username",
                    "email",
                    "password",
                    "user_role",
                    "rating",
                    "rating_deviation",
                    "rating_volatility",
                    "puzzles_rating",
                    "puzzles_rating_deviation",
                    "puzzles_rating_volatility",
                    "is_enable",
                    "creation_date",
                    "last_updated_date"
            )
            .values(11)
            .build();

    static final String INSERT_NEW_PARTNERSHIP = insert()
            .into("UserPartnership")
            .columns("user_id", "partner_id")
            .values(2)
            .build();

    static final String INSERT_USER_TOKEN = insert()
            .into("UserToken")
            .columns("id",
                    "user_id",
                    "token",
                    "is_confirmed",
                    "creation_date",
                    "expiration_date"
            )
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
                """
            )
            .where("id = ?")
            .build();

    static final String UPDATE_USER_PUZZLES_RATING = update("UserAccount")
            .set("""
                puzzles_rating = ?,
                puzzles_rating_deviation = ?,
                puzzles_rating_volatility = ?
                """
            )
            .where("id = ?")
            .build();

    static final String UPDATE_USER_TOKEN_AND_ACCOUNT = String.format("%s; %s;",
            update("UserToken")
            .set("is_confirmed = ?")
            .where("id = ?")
            .build(),
            update("UserAccount")
            .set("is_enable = ?, user_role = ?")
            .where("id = ?")
            .build()
    );

    static final String DELETE_USER_TOKEN_AND_ACCOUNT = String.format("%s; %s;",
            delete()
            .from("UserToken")
            .where("id = ?")
            .build(),
            delete()
            .from("UserAccount")
            .where("id = ?")
            .build()
    );

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
            userAccount.getPuzzlesRating().rating(),
            userAccount.getPuzzlesRating().ratingDeviation(),
            userAccount.getPuzzlesRating().volatility(),
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
    public void updateOfPuzzleRating(final UserAccount userAccount) {
        Rating puzzlesRating = userAccount.getPuzzlesRating();
        jdbc.write(UPDATE_USER_PUZZLES_RATING,
                puzzlesRating.rating(),
                puzzlesRating.ratingDeviation(),
                puzzlesRating.volatility(),
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
