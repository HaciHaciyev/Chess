package core.project.chess.infrastructure.security;

import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.entities.User;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class JWTUtility {

    private final JWTParser jwtParser;

    public JWTUtility(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public String generateToken(User user) {
        if (!user.isEnable())
            throw new IllegalStateException("It is forbidden to generate a token for an unverified user.");

        Duration expiration = Duration.ofDays(1).plusSeconds(1);
        return Jwt.issuer("Chessland")
                .upn(user.username())
                .expiresIn(expiration)
                .sign();
    }

    public String refreshToken(User user) {
        if (!user.isEnable())
            throw new IllegalStateException("It is forbidden to generate a token for an unverified user.");

        Duration year = Duration.ofDays(365);
        return Jwt.issuer("Chessland")
                .upn(user.username())
                .expiresIn(year)
                .sign();
    }

    /**
     * Extracts a JWT token from the WebSocket session request parameters.
     * <p>
     * WARNING: This method does NOT validate whether the token is expired.
     * Callers must explicitly check the 'exp' claim to ensure the token is still valid.
     */
    public Result<JsonWebToken, IllegalStateException> extractJWT(Session session) {
        List<String> token = session.getRequestParameterMap().get("token");
        if (Objects.isNull(token)) return Result.failure(new IllegalStateException("Token is missing."));
        if (token.isEmpty()) return Result.failure(new IllegalStateException("Token is missing."));
        try {
            JsonWebToken jwt = jwtParser.parse(token.getFirst());
            return Result.success(jwt);
        } catch (ParseException e) {
            return Result.failure(new IllegalStateException("Token is missing or invalid."));
        }
    }

    /**
     * Parses a JWT string into a JsonWebToken.
     * <p>
     * WARNING: This method does NOT validate whether the token is expired.
     * Callers must explicitly check the 'exp' claim to ensure the token is still valid.
     */
    public Optional<JsonWebToken> parseJWT(String token) {
        try {
            return Optional.of(jwtParser.parse(token));
        } catch (ParseException e) {
            Log.error("Can`t parse jwt.", e);
            return Optional.empty();
        }
    }
}