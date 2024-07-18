package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void save(UserAccount userAccount) {
        jdbcTemplate.update("""
                    INSERT INTO UserAccount
                        (id, username, email, password,
                        rating, is_enable, creation_date,
                        last_updated_date)
                        VALUES (?,?,?,?,?,?,?,?)
                    """,
                userAccount.getId().toString(), userAccount.getUsername(),
                userAccount.getEmail().email(), userAccount.getPassword(),
                userAccount.getRating().rating(), userAccount.isEnabled(),
                userAccount.getAccountEvents().creationDate(),
                userAccount.getAccountEvents().lastUpdateDate()
        );

        log.info("New user created : {}", userAccount.getUsername());
    }

    @Override
    @Transactional
    public void saveUserToken(EmailConfirmationToken token) {
        jdbcTemplate.update("""
                    INSERT INTO UserToken
                        (id, user_id, token, is_confirmed,
                        creation_date, expiration_date)
                        VALUES (?,?,?,?,?,?)
                    """,
                token.getTokenId().toString(), token.getUserAccount().getId().toString(),
                token.getToken().token().toString(), Boolean.FALSE,
                token.getTokenEvents().getCreationDate(), token.getTokenEvents().getExpirationDate()
        );

        log.info("Email verification token was saved");
    }

    @Override
    @Transactional
    public void enable(EmailConfirmationToken token) {
        try {
            if (!token.isConfirmed() || !token.getUserAccount().isEnabled()) {
                throw new IllegalAccessException("Token need to be confirmed & UserAccount need to be enabled");
            }

            jdbcTemplate.update("""
                            UPDATE UserToken SET
                                is_confirmed = ?
                            Where id = ?
                            """,
                    token.getUserAccount().isEnabled(), token.getUserAccount().getId()
            );

            jdbcTemplate.update("""
                            UPDATE UserAccount SET
                               is_enable = ?
                            WHERE id = ?
                            """,
                    token.isConfirmed(), token.getTokenId()
            );

            log.info("User account {} has became available", token);
        } catch (EmptyResultDataAccessException | IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException {
        Boolean isEnable = jdbcTemplate.queryForObject(
                "SELECT is_enable FROM UserAccount WHERE id = ?",
                (rs, _) -> rs.getBoolean("is_enable"),
                token.getUserAccount().getId().toString()
        );

        if (Boolean.TRUE.equals(isEnable) || token.isConfirmed()) {
            throw new IllegalAccessException("It is prohibited to delete an accessible account");
        }

        jdbcTemplate.update(
                "DELETE FROM UserToken WHERE id = ?", token.getTokenId().toString()
        );

        jdbcTemplate.update(
                "DELETE FROM UserAccount WHERE id = ?", token.getUserAccount().getId().toString()
        );
    }
}
