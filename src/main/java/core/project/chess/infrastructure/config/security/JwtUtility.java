package core.project.chess.infrastructure.config.security;

import core.project.chess.domain.aggregates.user.value_objects.Username;
import io.quarkus.logging.Log;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Singleton;

import java.util.concurrent.TimeUnit;

@Singleton
public class JwtUtility {

    public String generateToken(Username username) {
        Log.info("New token generation.");

        return Jwt.issuer("https://example.com/issuer")
                .subject(username.username())
                .upn("chessland")
                .expiresIn(TimeUnit.MINUTES.toMinutes(60))
                .claim("Username", username.username())
                .sign();
    }
}