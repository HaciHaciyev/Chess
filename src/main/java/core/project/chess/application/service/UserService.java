package core.project.chess.application.service;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.config.security.PasswordEncoder;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserService {

    private final JsonWebToken jwt;

    private final JWTParser jwtParser;

    private final JwtUtility jwtUtility;

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final EmailInteractionService emailInteractionService;

    public static final String NOT_ENABLED = "This account is not enabled.";

    public static final String INVALID_PASSWORD = "Invalid password. Must contains at least 8 characters.";

    private static final String USER_NOT_FOUND = "User %s not found, check data for correctness or register account if you do not have.";

    public static final String INVALID_USERNAME = "Invalid username. Username can`t be blank an need to contain only letters and digits, no special symbols";

    public String login(LoginForm loginForm) {
        final Username username = Result.ofThrowable(
                () -> new Username(loginForm.username())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_USERNAME).build())
        );

        final Password password = Result.ofThrowable(
                () -> new Password(loginForm.password())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_PASSWORD).build())
        );

        Log.debugf("Fetching user %s from repo", username.username());
        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                .entity(String.format(USER_NOT_FOUND, username.username()))
                                .build())
                );

        if (!userAccount.isEnabled()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(NOT_ENABLED).build());
        }

        final boolean isPasswordsMatch = passwordEncoder.verify(password, userAccount.getPassword());
        if (!isPasswordsMatch) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid password.").build());
        }

        Log.info("Login successful");
        final String refreshToken = jwtUtility.refreshToken(userAccount);
        inboundUserRepository.saveRefreshToken(userAccount, refreshToken);

        return jwtUtility.generateToken(userAccount);
    }

    public void registration(RegistrationForm registrationForm) {
        if (!Objects.equals(
                registrationForm.password(), registrationForm.passwordConfirmation())
        ) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Passwords don`t match.").build());
        }

        Username username = Result.ofThrowable(
                () -> new Username(registrationForm.username())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_USERNAME).build())
        );

        Email email = Result.ofThrowable(
                () -> new Email(registrationForm.email())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid email.").build())
        );

        Password password = Result.ofThrowable(
                () -> new Password(passwordEncoder.encode(new Password(registrationForm.password())))
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_PASSWORD).build())
        );

        if (outboundUserRepository.isUsernameExists(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Username already exists.").build());
        }

        if (outboundUserRepository.isEmailExists(email)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Email already exists.").build());
        }

        UserAccount userAccount = Result.ofThrowable(() ->
                UserAccount.of(username, email, password)
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid user account.").build())
        );

        Log.infof("Saving new user %s to repo", username.username());
        inboundUserRepository.save(userAccount);

        var token = EmailConfirmationToken.createToken(userAccount);

        Log.infof("Saving new token %s to repo", token.getToken().token().toString().substring(4));
        inboundUserRepository.saveUserToken(token);

        String link = String.format("/token/verification?%s", token.getToken());
        emailInteractionService.sendToEmail(email, link);

        Log.info("Registration successful.");
    }

    public void tokenVerification(String token) {
        Log.info("Token verification process.");
        var foundToken = outboundUserRepository
                .findToken(UUID.fromString(token))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This token is not exists").build())
                );

        if (foundToken.isExpired()) {
            try {
                inboundUserRepository.deleteByToken(foundToken);
            } catch (IllegalAccessException e) {
                Log.error("Unexpected error while deleting token. Probably account or token is not disabled.", e);
            }

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token was expired. You need to register again.").build());
        }

        foundToken.confirm();
        foundToken.getUserAccount().enable();
        inboundUserRepository.enable(foundToken);
    }

    public String refreshToken(String refreshToken) {
        final Pair<String, String> foundedPairResult = outboundUserRepository
                .findRefreshToken(refreshToken)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This refresh token is not founded").build())
                );

        final JsonWebToken refreshJWT = parseJWT(foundedPairResult.getSecond());
        long tokenExpirationDate = refreshJWT.getExpirationTime();
        final var tokenExpiration = LocalDateTime.ofEpochSecond(tokenExpirationDate, 0, ZoneOffset.UTC);

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(tokenExpiration)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Refresh token is expired, you need to login,").build());
        }

        final UserAccount userAccount = outboundUserRepository
                .findById(UUID.fromString(foundedPairResult.getFirst()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User was`t founded.").build())
                );

        return jwtUtility.generateToken(userAccount);
    }

    private JsonWebToken parseJWT(String token) {
        JsonWebToken jsonWebToken = null;
        try {
            jsonWebToken = jwtParser.parse(token);
        } catch (ParseException e) {
            Log.error("Unexpected error. Can`t parse jwt.", e);
        }

        return jsonWebToken;
    }
}
