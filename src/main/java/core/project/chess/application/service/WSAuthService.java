package core.project.chess.application.service;

import core.project.chess.domain.commons.containers.Result;
import core.project.chess.infrastructure.security.JWTUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;

@ApplicationScoped
public class WSAuthService {

    private final JWTUtility jwtUtility;

    WSAuthService(JWTUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    public Result<JsonWebToken, IllegalStateException> validateToken(Session session) {
        Result<JsonWebToken, IllegalStateException> parseResult = jwtUtility.extractJWT(session);
        if (!parseResult.success()) return parseResult;

        JsonWebToken token = parseResult.value();
        Instant expiration = Instant.ofEpochSecond(token.getExpirationTime());
        if (expiration.isBefore(Instant.now())) return Result.failure(new IllegalStateException("Token is expired."));
        return parseResult;
    }
}
