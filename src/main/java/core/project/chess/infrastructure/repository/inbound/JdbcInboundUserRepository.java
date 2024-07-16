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

import java.util.UUID;

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
                userAccount.getRating().rating(), userAccount.getIsEnable(),
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
                        (id, user_id, token,
                        creation_date, expiration_date)
                        VALUES (?,?,?,?,?)
                    """,
                token.tokenId().toString(),
                token.userAccount().getId().toString(), token.token().toString(),
                token.tokenEvents().getCreationDate(), token.tokenEvents().getExpirationDate()
        );

        log.info("Email verification token was saved");
    }

    @Override
    @Transactional
    public void enable(UUID userId) {
        try {
            jdbcTemplate.update("""
                            UPDATE UserAccount SET
                               is_enable = ?
                            WHERE id = ?
                            """,
                    Boolean.TRUE, userId.toString()
            );

            log.info(String.format("User account %s has became available", userId));
        } catch (EmptyResultDataAccessException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException {
        Boolean isEnable = jdbcTemplate.queryForObject(
                "SELECT is_enable FROM UserAccount WHERE id = ?",
                Boolean.class,
                token.userAccount().getId().toString()
        );

        if (Boolean.TRUE.equals(isEnable)) {
            throw new IllegalAccessException("It is prohibited to delete an accessible account");
        }

        jdbcTemplate.update(
                "DELETE FROM UserToken WHERE id = ?", token.tokenId().toString()
        );

        jdbcTemplate.update(
                "DELETE FROM UserAccount WHERE id = ?", token.userAccount().getId().toString()
        );
    }
}
