package testUtils;

import core.project.chess.infrastructure.config.jdbc.JDBC;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserDBManagement {

    private final JDBC jdbc;

    private static final String DELETE_ALL_USERS_CASCADE = """
            DELETE FROM UserToken;
            DELETE FROM UserAccount
            """;

    private static final String GET_TOKEN_BY_USERNAME = """
            SELECT token FROM UserToken
            WHERE user_id=(SELECT id FROM UserAccount where username=?)
            """;

    public UserDBManagement(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    public void removeUsers() {
        jdbc.write(DELETE_ALL_USERS_CASCADE);
    }

    public String getToken(String username) {
        Result<String, Throwable> tokenResult = jdbc.read(GET_TOKEN_BY_USERNAME, rs -> rs.getString("token"), username);
        return tokenResult.orElseThrow();
    }
}
