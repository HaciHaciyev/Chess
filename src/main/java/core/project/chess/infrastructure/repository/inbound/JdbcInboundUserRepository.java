package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
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
                userAccount.getRating().rating(), userAccount.getEventsOfAccount().creationDate(),
                userAccount.getEventsOfAccount().lastUpdateDate()
        );
    }

    @Override
    public void saveUserToken(EmailConfirmationToken token) {
        jdbcTemplate.update("""
                    INSERT INTO UserToken
                        (id, user_id, token, creation_date)
                        VALUES (?,?,?,?)
                    """,
                token.tokenId().toString(),
                token.userAccount().getId().toString(),
                token.token().toString(), token.creationDate()
        );
    }

    @Override
    public void enable(UUID userId) {
        jdbcTemplate.update("""
                    UPDATE UserAccount SET
                       is_enable = ?
                    WHERE id = ?
                    """,
                Boolean.TRUE, userId.toString()
        );
    }
}
