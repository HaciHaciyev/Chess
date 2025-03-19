package core.project.chess.application.service;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.*;
import core.project.chess.infrastructure.security.JwtUtility;
import core.project.chess.infrastructure.security.PasswordEncoder;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static core.project.chess.application.util.JSONUtilities.responseException;

@ApplicationScoped
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

    public static final String EMAIL_VERIFICATION_URL = "http://localhost:8080/chessland/account/token/verification?token=%s";

    UserAuthService(JWTParser jwtParser, JwtUtility jwtUtility, PasswordEncoder passwordEncoder,
                    InboundUserRepository inboundUserRepository, OutboundUserRepository outboundUserRepository,
                    EmailInteractionService emailInteractionService) {
        this.jwtParser = jwtParser;
        this.jwtUtility = jwtUtility;
        this.passwordEncoder = passwordEncoder;
        this.inboundUserRepository = inboundUserRepository;
        this.outboundUserRepository = outboundUserRepository;
        this.emailInteractionService = emailInteractionService;
    }

    public Map<String, String> login(LoginForm loginForm) {
        try {
            Log.infof("User %s is logging in", loginForm.username());
            if (!Password.validate(loginForm.password())) {
                throw responseException(Response.Status.BAD_REQUEST, "Invalid password.");
            }

            final Username username = new Username(loginForm.username());
            final Password password = new Password(loginForm.password());

            final UserAccount userAccount = outboundUserRepository
                    .findByUsername(username)
                    .orElseThrow(() -> {
                        Log.error("Login failure, user not found");
                        return responseException(Response.Status.NOT_FOUND, String.format(USER_NOT_FOUND, username.username()));
                    });

            if (!userAccount.isEnabled()) {
                Log.error("Login failure, account is not enabled");
                throw responseException(Response.Status.BAD_REQUEST, NOT_ENABLED);
            }

            final boolean isPasswordsMatch = passwordEncoder.verify(password, userAccount.getPassword());
            if (!isPasswordsMatch) {
                Log.error("Login failure, wrong password");
                throw responseException(Response.Status.BAD_REQUEST, "Invalid password.");
            }

            Log.info("Login successful");
            final String refreshToken = jwtUtility.refreshToken(userAccount);
            inboundUserRepository.saveRefreshToken(userAccount, refreshToken);

            final String token = jwtUtility.generateToken(userAccount);
            return Map.of("token", token, "refreshToken", refreshToken);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public void registration(RegistrationForm registrationForm) {
        try {
            Log.infof("Starting registration process for user %s", registrationForm.username());
            if (!Password.validate(registrationForm.password())) {
                throw responseException(Response.Status.BAD_REQUEST, "Invalid password.");
            }

            if (!Objects.equals(registrationForm.password(), registrationForm.passwordConfirmation())) {
                Log.error("Registration failure, passwords do not match");
                throw responseException(Response.Status.BAD_REQUEST, "Passwords do not match");
            }

            Username username = new Username(registrationForm.username());
            Email email = new Email(registrationForm.email());
            Firstname firstname = new Firstname(registrationForm.firstname());
            Surname surname = new Surname(registrationForm.surname());
            Password password = new Password(passwordEncoder.encode(new Password(registrationForm.password())));

            if (outboundUserRepository.isUsernameExists(username)) {
                Log.errorf("Registration failure, user %s already exists", username.username());
                throw responseException(Response.Status.BAD_REQUEST, "Username already exists.");
            }

            if (outboundUserRepository.isEmailExists(email)) {
                Log.errorf("Registration failure, email %s already exists", email.email());
                throw responseException(Response.Status.BAD_REQUEST, "Email already exists.");
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
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public void tokenVerification(String token) {
        Log.infof("Verifying %s", token);
        var foundToken = outboundUserRepository
                .findToken(UUID.fromString(token))
                .orElseThrow(() -> {
                    Log.error("Verification failure, token does not exist");
                    return responseException(Response.Status.NOT_FOUND, "This token does not exist.");
                });

        if (foundToken.isExpired()) {
            Log.error("Verification failure, token expired");
            try {
                Log.infof("Deleting user %s", foundToken.getUserAccount().getUsername().username());
                inboundUserRepository.deleteByToken(foundToken);
            } catch (IllegalAccessException e) {
                Log.error("Can't delete enabled account", e);
            }

            throw responseException(Response.Status.BAD_REQUEST, "Token was expired. You need to register again.");
        }

        foundToken.confirm();
        foundToken.getUserAccount().enable();
        inboundUserRepository.enable(foundToken);
        Log.infof("Verification successful");
    }

    public String refreshToken(String refreshToken) {
        final Pair<String, String> foundedPairResult = outboundUserRepository.findRefreshToken(refreshToken)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "This refresh token is not found."));

        long tokenExpirationDate = parseJWT(foundedPairResult.getSecond())
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "Something went wrong, try again later."))
                .getExpirationTime();

        final var tokenExpiration = LocalDateTime.ofEpochSecond(tokenExpirationDate, 0, ZoneOffset.UTC);

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(tokenExpiration)) {
            throw responseException(Response.Status.BAD_REQUEST, "Refresh token is expired, you need to login.");
        }

        final UserAccount userAccount = outboundUserRepository
                .findById(UUID.fromString(foundedPairResult.getFirst()))
                .orElseThrow(() -> {
                    Log.error("User is not found");
                    return responseException(Response.Status.NOT_FOUND, "User not found.");
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
