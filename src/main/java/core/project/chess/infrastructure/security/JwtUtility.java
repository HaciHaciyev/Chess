package core.project.chess.infrastructure.security;

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
public class JwtUtility {

    private final JWTParser jwtParser;

    public JwtUtility(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public String generateToken(User user) {
        Duration expiration = Duration.ofDays(1).plusSeconds(1);

        return Jwt.issuer("Chessland")
                .upn(user.username())
                .expiresIn(expiration)
                .sign();
    }

    public String refreshToken(User user) {
        Duration year = Duration.ofDays(365);

        return Jwt.issuer("Chessland")
                .upn(user.username())
                .expiresIn(year)
                .sign();
    }

    public Optional<JsonWebToken> extractJWT(Session session) {
        List<String> token = session.getRequestParameterMap().get("token");
        if (Objects.isNull(token)) return Optional.empty();
        if (token.isEmpty()) return Optional.empty();
        try {
            return Optional.of(jwtParser.parse(token.getFirst()));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    public Optional<JsonWebToken> parseJWT(String token) {
        try {
            return Optional.of(jwtParser.parse(token));
        } catch (ParseException e) {
            Log.error("Can`t parse jwt.", e);
        }

        return Optional.empty();
    }
}