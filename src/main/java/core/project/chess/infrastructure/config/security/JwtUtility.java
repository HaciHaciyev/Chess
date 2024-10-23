package core.project.chess.infrastructure.config.security;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Objects;

@Singleton
public class JwtUtility {

    private final JWTParser jwtParser;

    public JwtUtility(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public String generateToken(UserAccount userAccount) {
        Log.info("New token generation.");

        return Jwt.issuer("Chessland")
                .upn(userAccount.getUsername().username())
                .groups(userAccount.getUserRole().getUserRole())
                .expiresIn(86401)
                .sign();
    }

    public String refreshToken(UserAccount userAccount) {
        Log.info("New token generation.");

        final long year = 86400L * 365L + 1L;
        return Jwt.issuer("Chessland")
                .upn(userAccount.getUsername().username())
                .groups(userAccount.getUserRole().getUserRole())
                .expiresIn(year)
                .sign();
    }

    public JsonWebToken extractJWT(final Session session) {
        final String token = session.getRequestParameterMap().get("token").getFirst();
        if (Objects.isNull(token)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        try {
            return jwtParser.parse(token);
        } catch (ParseException e) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid JWT token.").build());
        }
    }
}