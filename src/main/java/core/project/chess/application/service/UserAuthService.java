package core.project.chess.application.service;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.*;
import core.project.chess.infrastructure.security.JwtUtility;
import core.project.chess.infrastructure.security.PasswordEncoder;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserAuthService {

    private final JWTParser jwtParser;

    private final JwtUtility jwtUtility;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final EmailInteractionService emailInteractionService;

    public static final String NOT_ENABLED = "This account is not enabled.";

    public static final String INVALID_PASSWORD = "Invalid password. Must contains at least 8 characters.";

    private static final String USER_NOT_FOUND = "User %s not found, check data for correctness or register account if you do not have.";

    public static final String INVALID_USERNAME = "Invalid username. Username can`t be blank an need to contain only letters and digits, no special symbols";

    public static final String EMAIL_VERIFICATION_URL = "http://localhost:8080/chessland/account/token/verification?token=%s";

    public Map<String, String> login(LoginForm loginForm) {
        Log.infof("User %s is logging in", loginForm.username());
        if (!Password.validate(loginForm.password())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Password is not valid").build());
        }

        final Username username = Result.ofThrowable(() -> new Username(loginForm.username())).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_USERNAME).build())
        );

        final Password password = Result.ofThrowable(() -> new Password(loginForm.password())).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_PASSWORD).build())
        );

        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(() -> {
                    Log.error("Login failure, user not found");
                    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity(String.format(USER_NOT_FOUND, username.username()))
                            .build());
                });

        if (!userAccount.isEnabled()) {
            Log.error("Login failure, account is not enabled");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(NOT_ENABLED).build());
        }

        final boolean isPasswordsMatch = passwordEncoder.verify(password, userAccount.getPassword());
        if (!isPasswordsMatch) {
            Log.error("Login failure, wrong password");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid password.").build());
        }

        Log.info("Login successful");
        final String refreshToken = jwtUtility.refreshToken(userAccount);
        inboundUserRepository.saveRefreshToken(userAccount, refreshToken);

        final String token = jwtUtility.generateToken(userAccount);
        return Map.of("token", token, "refreshToken", refreshToken);
    }

    public void registration(RegistrationForm registrationForm) {
        Log.infof("Starting registration process for user %s", registrationForm.username());
        if (!Password.validate(registrationForm.password())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Password is not valid").build());
        }

        if (!Objects.equals(registrationForm.password(), registrationForm.passwordConfirmation())) {
            Log.error("Registration failure, passwords do not match");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Passwords don`t match.").build());
        }

        Username username = Result.ofThrowable(() -> new Username(registrationForm.username())).orElseThrow(() -> {
            Log.error("Registration failure, invalid username");
            return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_USERNAME).build());
        });

        Email email = Result.ofThrowable(() -> new Email(registrationForm.email())).orElseThrow(() -> {
            Log.error("Registration failure, invalid email");
            return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid email.").build());
        });

        Firstname firstname = Result.ofThrowable(() -> new Firstname(registrationForm.firstname())).orElseThrow(() -> {
            Log.error("Registration failure, invalid firstname");
            return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid firstname.").build());
        });

        Surname surname = Result.ofThrowable(() -> new Surname(registrationForm.surname())).orElseThrow(() -> {
            Log.error("Registration failure, invalid surname");
            return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid surname.").build());
        });

        Password password = Result.ofThrowable(() -> new Password(passwordEncoder.encode(new Password(registrationForm.password())))).orElseThrow(() -> {
            Log.errorf("Registration failure. Invalid Password.");
            return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(INVALID_PASSWORD).build());
        });

        if (outboundUserRepository.isUsernameExists(username)) {
            Log.errorf("Registration failure, user %s already exists", username.username());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Username already exists.").build());
        }

        if (outboundUserRepository.isEmailExists(email)) {
            Log.errorf("Registration failure, email %s already exists", email.email());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Email already exists.").build());
        }

        UserAccount userAccount = UserAccount.of(firstname, surname, username, email, password);

        Log.infof("Saving user %s to repo", username.username());
        inboundUserRepository.save(userAccount);

        EmailConfirmationToken token = EmailConfirmationToken.createToken(userAccount);
        Log.info("Saving email token to repo");
        inboundUserRepository.saveUserToken(token);

        String link = EMAIL_VERIFICATION_URL.formatted(token.getToken().token());

        Log.infof("Sending verification link to %s", username.username());
        emailInteractionService.sendToEmail(email, link);

        Log.info("Registration successful");
    }

    public void tokenVerification(String token) {
        Log.infof("Verifying %s", token);
        var foundToken = outboundUserRepository
                .findToken(UUID.fromString(token))
                .orElseThrow(() -> {
                    Log.error("Verification failure, token does not exist");
                    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This token does not exist").build());
                });

        if (foundToken.isExpired()) {
            Log.error("Verification failure, token expired");
            try {
                Log.infof("Deleting user %s", foundToken.getUserAccount().getUsername().username());
                inboundUserRepository.deleteByToken(foundToken);
            } catch (IllegalAccessException e) {
                Log.error("Can't delete enabled account", e);
            }

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token was expired. You need to register again.").build());
        }

        foundToken.confirm();
        foundToken.getUserAccount().enable();
        inboundUserRepository.enable(foundToken);
        Log.infof("Verification successful");
    }

    public String refreshToken(String refreshToken) {
        final Pair<String, String> foundedPairResult = outboundUserRepository
                .findRefreshToken(refreshToken)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This refresh token is not found").build())
                );

        Optional<JsonWebToken> refreshJWT = parseJWT(foundedPairResult.getSecond());
        long tokenExpirationDate = refreshJWT.orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Something went wrong, try again later").build()))
                .getExpirationTime();

        final var tokenExpiration = LocalDateTime.ofEpochSecond(tokenExpirationDate, 0, ZoneOffset.UTC);

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(tokenExpiration)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Refresh token is expired, you need to login,").build());
        }

        final UserAccount userAccount = outboundUserRepository
                .findById(UUID.fromString(foundedPairResult.getFirst()))
                .orElseThrow(() -> {
                    Log.error("User is not found");
                    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User was`t founded.").build());
                });

        return jwtUtility.generateToken(userAccount);
    }

    private Optional<JsonWebToken> parseJWT(String token) {
        try {
            return Optional.of(jwtParser.parse(token));
        } catch (ParseException e) {
            Log.error("Can`t parse jwt.", e);
        }

        return Optional.empty();
    }
}
