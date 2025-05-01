package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.infrastructure.security.JWTUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.Optional;

import static core.project.chess.application.util.WSUtilities.closeSession;

@ApplicationScoped
public class WSAuthService {

    private final JWTUtility jwtUtility;

    WSAuthService(JWTUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    public Optional<JsonWebToken> validateToken(Session session) {
        Optional<JsonWebToken> jwt = jwtUtility.extractJWT(session);

        if (jwt.isEmpty()) {
            closeSession(session, Message.error("You are not authorized. Token is required."));
            return Optional.empty();
        }

        JsonWebToken token = jwt.get();
        Instant expiration = Instant.ofEpochSecond(token.getExpirationTime());
        if (expiration.isBefore(Instant.now())) {
            closeSession(session, Message.error("You are not authorized. Token has expired. Session closing."));
            return Optional.empty();
        }

        return jwt;
    }
}
