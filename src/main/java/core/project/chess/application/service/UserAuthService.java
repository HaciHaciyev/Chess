package core.project.chess.application.service;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Password;
import core.project.chess.domain.user.value_objects.PersonalData;
import core.project.chess.domain.user.value_objects.RefreshToken;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.security.JwtUtility;
import core.project.chess.infrastructure.security.PasswordEncoder;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static core.project.chess.application.util.JSONUtilities.responseException;

@ApplicationScoped
public class UserAuthService {

    private final JwtUtility jwtUtility;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final EmailInteractionService emailInteractionService;

    public static final String NOT_ENABLED = "This account is not enabled.";

    private static final String NOT_FOUND = "User %s not found, check data for correctness or register account if you do not have.";

    public static final String EMAIL_VERIFICATION_URL = "http://localhost:8080/chessland/account/token/verification?token=%s";

    UserAuthService(JwtUtility jwtUtility,
                    PasswordEncoder passwordEncoder,
                    InboundUserRepository inboundUserRepository,
                    OutboundUserRepository outboundUserRepository,
                    EmailInteractionService emailInteractionService) {
        this.jwtUtility = jwtUtility;
        this.passwordEncoder = passwordEncoder;
        this.inboundUserRepository = inboundUserRepository;
        this.outboundUserRepository = outboundUserRepository;
        this.emailInteractionService = emailInteractionService;
    }

    public void registration(RegistrationForm registrationForm) {
        try {
            Password.validate(registrationForm.password());

            if (!Objects.equals(registrationForm.password(), registrationForm.passwordConfirmation())) {
                Log.errorf("Registration failure, passwords do not match for user %s", registrationForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Passwords do not match");
            }

            PersonalData personalData = new PersonalData(
                    registrationForm.firstname(),
                    registrationForm.surname(),
                    registrationForm.username(),
                    registrationForm.email(),
                    passwordEncoder.encode(registrationForm.password())
            );

            if (outboundUserRepository.isUsernameExists(registrationForm.username())) {
                Log.errorf("Registration failure, user %s already exists", registrationForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Username already exists.");
            }

            if (outboundUserRepository.isEmailExists(registrationForm.email())) {
                Log.errorf("Registration failure, email %s of user %s already exists",
                        registrationForm.email(), registrationForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Email already exists.");
            }

            User user = User.of(personalData);
            inboundUserRepository.save(user);

            EmailConfirmationToken token = EmailConfirmationToken.createToken(user);
            inboundUserRepository.saveUserToken(token);

            String link = EMAIL_VERIFICATION_URL.formatted(token.token().token());

            emailInteractionService.sendToEmail(personalData.email(), link);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public void verification(String token) {
        try {
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
                    Log.infof("Deleting user %s", foundToken.user().username());
                    inboundUserRepository.deleteByToken(foundToken);
                } catch (IllegalAccessException e) {
                    Log.error("Can't delete enabled account", e);
                }

                throw responseException(Response.Status.BAD_REQUEST, "Token was expired. You need to register again.");
            }

            foundToken.confirm();
            foundToken.user().enable();
            inboundUserRepository.enable(foundToken);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public Map<String, String> login(LoginForm loginForm) {
        try {
            Password.validate(loginForm.password());
            Username.validate(loginForm.username());

            final User user = outboundUserRepository
                    .findByUsername(loginForm.username())
                    .orElseThrow(() -> {
                        Log.errorf("Login failure, user %s not found", loginForm.username());
                        return responseException(Response.Status.NOT_FOUND, String.format(NOT_FOUND, loginForm.username()));
                    });

            if (!user.isEnabled()) {
                Log.errorf("Login failure, account %s is not enabled", loginForm.username());
                throw responseException(Response.Status.BAD_REQUEST, NOT_ENABLED);
            }

            final boolean isPasswordsMatch = passwordEncoder.verify(loginForm.password(), user.password());
            if (!isPasswordsMatch) {
                Log.errorf("Login failure, wrong password for user %s", loginForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Invalid password.");
            }

            final String refreshToken = jwtUtility.refreshToken(user);
            inboundUserRepository.saveRefreshToken(user, refreshToken);

            final String token = jwtUtility.generateToken(user);
            return Map.of("token", token, "refreshToken", refreshToken);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public String refreshToken(String refreshToken) {
        final RefreshToken foundedPairResult = outboundUserRepository.findRefreshToken(refreshToken)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "This refresh token is not found."));

        long tokenExpirationDate = jwtUtility.parseJWT(foundedPairResult.refreshToken())
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "Something went wrong, try again later."))
                .getExpirationTime();

        final var tokenExpiration = LocalDateTime.ofEpochSecond(tokenExpirationDate, 0, ZoneOffset.UTC);

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(tokenExpiration)) {
            inboundUserRepository.removeRefreshToken(refreshToken);
            throw responseException(Response.Status.BAD_REQUEST, "Refresh token is expired, you need to login.");
        }

        final User user = outboundUserRepository
                .findById(foundedPairResult.userID())
                .orElseThrow(() -> {
                    Log.error("User is not found");
                    return responseException(Response.Status.NOT_FOUND, "User not found.");
                });

        return jwtUtility.generateToken(user);
    }
}
