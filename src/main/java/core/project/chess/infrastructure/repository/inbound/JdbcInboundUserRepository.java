package core.project.chess.infrastructure.repository.inbound;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.utilities.Result;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JdbcInboundUserRepository implements InboundUserRepository {

    private final JDBC jdbc;

    JdbcInboundUserRepository(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UserAccount userAccount) {
        final String sql = """
                    INSERT INTO UserAccount
                        (id, username, email, password,
                        rating, is_enable, creation_date,
                        last_updated_date)
                        VALUES (?,?,?,?,?,?,?,?)
                    """;

        Result<Boolean, Throwable> result = jdbc.update(sql, userAccount.getId().toString(), userAccount.getUsername().username(),
                userAccount.getEmail().email(), userAccount.getPassword().password(),
                userAccount.getRating().rating(), userAccount.isEnable(),
                userAccount.getAccountEvents().creationDate(),
                userAccount.getAccountEvents().lastUpdateDate()
        );

        result.ifFailure(Throwable::printStackTrace);
    }

    @Override
    public void saveUserToken(EmailConfirmationToken token) {

    }

    @Override
    public void enable(EmailConfirmationToken token) {

    }

    @Override
    public void deleteByToken(EmailConfirmationToken token) throws IllegalAccessException {

    }
}
