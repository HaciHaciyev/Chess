package core.project.chess.infrastructure.config.security;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import io.quarkus.logging.Log;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Singleton;

@Singleton
public class JwtUtility {

    public String generateToken(UserAccount userAccount) {
        Log.info("New token generation.");

        return Jwt.issuer("Chessland")
                .upn(userAccount.getUsername().username())
                .groups(userAccount.getUserRole().getUserRole())
                .expiresIn(86401)
                .sign();
    }
}