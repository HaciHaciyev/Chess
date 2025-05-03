package core.project.chess.application.service;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.domain.user.entities.EmailConfirmationToken;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.*;
import core.project.chess.infrastructure.security.JWTUtility;
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
public class AuthService {

    private final JWTUtility jwtUtility;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final EmailInteractionService emailInteractionService;

    public static final String NOT_ENABLED = "This account is not enabled.";

    private static final String NOT_FOUND = "User %s not found, check data for correctness or register account if you do not have.";

    public static final String EMAIL_VERIFICATION_URL = "http://localhost:8080/chessland/account/token/verification?token=%s";

    AuthService(JWTUtility jwtUtility,
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

            if (outboundUserRepository.isUsernameExists(new Username(registrationForm.username()))) {
                Log.errorf("Registration failure, user %s already exists", registrationForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Username already exists.");
            }

            if (outboundUserRepository.isEmailExists(new Email(registrationForm.email()))) {
                Log.errorf("Registration failure, email %s of user %s already exists",
                        registrationForm.email(), registrationForm.username());
                throw responseException(Response.Status.BAD_REQUEST, "Email already exists.");
            }

            User user = User.of(personalData);
            inboundUserRepository.save(user);

            EmailConfirmationToken token = EmailConfirmationToken.createToken(user);
            inboundUserRepository.saveVerificationToken(token);

            String link = EMAIL_VERIFICATION_URL.formatted(token.token().token());
            emailInteractionService.sendToEmail(personalData.email(), link);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public void resendVerificationToken(String receivedEmail) {
        try {
            Email email = new Email(receivedEmail);

            User user = outboundUserRepository.findByEmail(email)
                    .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "User by this email is do not found."));

            if (user.isEnable()) throw responseException(Response.Status.CONFLICT, "User is already verified.");

            inboundUserRepository.removeVerificationToken(user);
            EmailConfirmationToken token = EmailConfirmationToken.createToken(user);
            inboundUserRepository.saveVerificationToken(token);
            String link = EMAIL_VERIFICATION_URL.formatted(token.token().token());
            emailInteractionService.sendToEmail(email.email(), link);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public void verification(String token) {
        try {
            var foundToken = outboundUserRepository
                    .findToken(UUID.fromString(token))
                    .orElseThrow(() -> {
                        Log.error("Verification failure, token does not exist");
                        return responseException(Response.Status.NOT_FOUND, "This token does not exist.");
                    });

            if (foundToken.isExpired())
                throw responseException(Response.Status.FORBIDDEN, "Token was expired.");

            if (foundToken.user().isEnable())
                throw responseException(Response.Status.FORBIDDEN, "User already verified.");

            foundToken.confirm();
            foundToken.user().enable();
            inboundUserRepository.updateUserVerification(foundToken);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    public Map<String, String> login(LoginForm loginForm) {
        try {
            Password.validate(loginForm.password());
            Username.validate(loginForm.username());

            final User user = outboundUserRepository
                    .findByUsername(new Username(loginForm.username()))
                    .orElseThrow(() -> {
                        Log.errorf("Login failure, user %s not found", loginForm.username());
                        return responseException(Response.Status.NOT_FOUND, String.format(NOT_FOUND, loginForm.username()));
                    });

            if (!user.isEnable()) {
                Log.errorf("Login failure, account %s is not enabled", loginForm.username());
                throw responseException(Response.Status.FORBIDDEN, NOT_ENABLED);
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
        } catch (IllegalStateException e) {
            throw responseException(Response.Status.FORBIDDEN, e.getMessage());
        }
    }

    public String refreshToken(String refreshToken) {
        try {
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
        } catch (IllegalArgumentException | NullPointerException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw responseException(Response.Status.FORBIDDEN, e.getMessage());
        }
    }
}
