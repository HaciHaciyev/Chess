package testUtils;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import core.project.chess.domain.commons.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserDBManagement {

    private final JetQuerious jet;

    private static final String DELETE_ALL_USERS_CASCADE = """
            DELETE FROM UserToken;
            DELETE FROM UserAccount
            """;

    private static final String GET_TOKEN_BY_USERNAME = """
            SELECT token FROM UserToken
            WHERE user_id=(SELECT id FROM UserAccount where username=?)
            """;

    public UserDBManagement() {
        this.jet = JetQuerious.instance();
    }

    public void removeUsers() {
        jet.write(DELETE_ALL_USERS_CASCADE);
    }

    public String getToken(String username) {
        var token = jet.read(GET_TOKEN_BY_USERNAME, rs -> rs.getString("token"), username);
        Result<String, Throwable> tokenResult = new Result<>(token.value(), token.throwable(), token.success());
        return tokenResult.orElseThrow();
    }
}
