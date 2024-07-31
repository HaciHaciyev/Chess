package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.events.TokenEvents;
import core.project.chess.domain.aggregates.user.value_objects.*;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdbcOutboundUserRepository implements OutboundUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isEmailExists(Email verifiableEmail) {
        try {
            String findEmail = "Select COUNT(email) from UserAccount where email = ?";
            Integer count = jdbcTemplate.queryForObject(
                    findEmail,
                    Integer.class,
                    verifiableEmail.email()
            );
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isUsernameExists(Username verifiableUsername) {
        try {
            String findEmail = "Select COUNT(username) from UserAccount where username = ?";
            Integer count = jdbcTemplate.queryForObject(
                    findEmail,
                    Integer.class,
                    verifiableUsername.username()
            );
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        try {
            String selectByUsername = "Select * from UserAccount where id = ?";

            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(selectByUsername, this::userAccountMapper, userId.toString())
            );
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<EmailConfirmationToken> findToken(UUID token) {
        try {
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

            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(selectUserToken, this::userTokenMapper, token.toString())
            );
        } catch (EmptyResultDataAccessException | NoSuchElementException e) {
            log.info(e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        try {
            String selectByUsername = "Select * from UserAccount where username = ?";

            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(selectByUsername, this::userAccountMapper, username.username())
            );
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        try {
            String selectByUsername = "Select * from UserAccount where email = ?";

            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(selectByUsername, this::userAccountMapper, email.email())
            );
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
            return Optional.empty();
        }
    }

    private EmailConfirmationToken userTokenMapper(ResultSet rs, int rowNum)
            throws SQLException {
        var tokenEvents = new TokenEvents(
                rs.getObject("token_creation_date", Timestamp.class).toLocalDateTime()
        );

        return EmailConfirmationToken.builder()
                .tokenId(UUID.fromString("token_id"))
                .userAccount(userAccountMapper(rs, rowNum))
                .token(new Token(UUID.fromString(rs.getString("token"))))
                .tokenEvents(tokenEvents)
                .confirmed(rs.getBoolean("token_confirmation"))
                .build();
    }

    private UserAccount userAccountMapper(ResultSet rs, int rowNum)
            throws SQLException {
        log.info("The user account {} was taken from the database", rs.getString("username"));

        var events = new AccountEvents(
                rs.getObject("creation_date", Timestamp.class).toLocalDateTime(),
                rs.getObject("last_updated_date", Timestamp.class).toLocalDateTime()
        );

        return UserAccount.fromRepo(
                UUID.fromString(rs.getString("id")),
                new Username(rs.getString("username")),
                new Email(rs.getString("email")),
                new Password(rs.getString("password")),
                new Password(rs.getString("password")),
                new Rating(rs.getShort("rating")),
                rs.getBoolean("is_enable"),
                events
        );
    }
}
