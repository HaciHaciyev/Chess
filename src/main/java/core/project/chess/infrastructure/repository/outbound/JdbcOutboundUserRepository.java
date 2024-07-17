package core.project.chess.infrastructure.repository.outbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.events.AccountEvents;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Rating;
import core.project.chess.domain.aggregates.user.value_objects.Username;
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
    // TODO for Nicat
    public Optional<EmailConfirmationToken> findTokenById(UUID tokenId) {
        try {

            // TODO for Nicat
            return Optional.empty();
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

    private UserAccount userAccountMapper(ResultSet rs, int rowNum)
            throws SQLException {
        log.info(
                String.format("The user account %s was taken from the database", rs.getString("username"))
        );

        var events = new AccountEvents(
                rs.getObject("creation_date", Timestamp.class).toLocalDateTime(),
                rs.getObject("last_updated_date", Timestamp.class).toLocalDateTime()
        );

        return UserAccount.builder()
                .id(UUID.fromString(rs.getString("id")))
                .username(new Username(rs.getString("username")))
                .email(new Email(rs.getString("email")))
                .password(new Password(rs.getString("password")))
                .passwordConfirm(new Password(rs.getString("password")))
                .rating(new Rating(rs.getShort("rating")))
                .enable(rs.getBoolean("is_enable"))
                .accountEvents(events)
                .build();
    }
}
