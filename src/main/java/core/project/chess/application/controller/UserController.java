package core.project.chess.application.controller;

import core.project.chess.application.dto.gamesession.ChessGameHistory;
import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.EmailInteractionService;
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
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.*;

@PermitAll
@Path("/account")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserController {

    private final JsonWebToken jwt;

    private final JwtUtility jwtUtility;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private final EmailInteractionService emailInteractionService;

    public static final String NOT_ENABLED = "This account is not enabled.";

    public static final String INVALID_PASSWORD = "Invalid password. Must contains at least 8 characters.";

    private static final String USER_NOT_FOUND = "User %s not found, check data for correctness or register account if you do not have.";

    public static final String INVALID_USERNAME = "Invalid username. Username can`t be blank an need to contain only letters and digits, no special symbols";

    @POST @Path("/login")
    public final Response login(LoginForm loginForm) {
        if (Objects.isNull(loginForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Login form is null.").build());
        }

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
        final String token = jwtUtility.generateToken(userAccount);
        return Response.ok(token).build();
    }

    @POST @Path("/registration")
    public final Response registration(RegistrationForm registrationForm) {
        if (Objects.isNull(registrationForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Registration form is null.").build());
        }

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
        return Response.ok("Registration successful. Verify you email.").build();
    }

    @PATCH @Path("/token/verification")
    public final Response tokenVerification(@QueryParam("token") String token) {
        if (Objects.isNull(token)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token can`t be null.").build());
        }

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

        return Response.ok().build();
    }

    @Authenticated
    @GET @Path("/game-history")
    public final Response gameHistory(@QueryParam("pageNumber") int pageNumber) {
        List<ChessGameHistory> listOfGames = outboundChessRepository
                .listOfGames(new Username(jwt.getName()), pageNumber)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Data not found.").build())
                );

        return Response.ok(listOfGames).build();
    }
}