package core.project.chess.application.controller;

import core.project.chess.application.model.LoginForm;
import core.project.chess.application.model.RegistrationForm;
import core.project.chess.application.service.EmailInteractionService;
import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.config.security.PasswordEncoder;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;
import java.util.UUID;

@PermitAll
@Path("/chess")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    private final JwtUtility jwtUtility;

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final EmailInteractionService emailInteractionService;

    UserController(JwtUtility jwtUtility, PasswordEncoder passwordEncoder, InboundUserRepository inboundUserRepository,
                   OutboundUserRepository outboundUserRepository, EmailInteractionService emailInteractionService) {
        this.jwtUtility = jwtUtility;
        this.passwordEncoder = passwordEncoder;
        this.inboundUserRepository = inboundUserRepository;
        this.outboundUserRepository = outboundUserRepository;
        this.emailInteractionService = emailInteractionService;
    }

    @POST @Path("/login")
    public final Response login(final LoginForm loginForm) {
        Objects.requireNonNull(loginForm);

        final Username username = Result.ofThrowable(
                () -> new Username(loginForm.username())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
        );

        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST).entity(String.format("User %s not found", username.username())).build()
                        )
                );

        if (!userAccount.isEnable()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This account is not enable for some reason.").build());
        }

        final boolean isPasswordsMatch = passwordEncoder.verify(new Password(loginForm.password()), userAccount.getPassword());
        if (!isPasswordsMatch) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid password.").build());
        }

        final String token = jwtUtility.generateToken(username);
        return Response.ok(token).build();
    }

    @POST @Path("/registration")
    public final Response registration(RegistrationForm registrationForm) {
        Objects.requireNonNull(registrationForm);

        if (!Objects.equals(
                registrationForm.password(), registrationForm.passwordConfirmation())
        ) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Passwords don`t match.").build());
        }

        Username username = Result.ofThrowable(
                () -> new Username(registrationForm.username())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
        );

        Email email = Result.ofThrowable(
                () -> new Email(registrationForm.email())
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid email.").build())
        );

        Password password = Result.ofThrowable(
                () -> new Password(passwordEncoder.encode(new Password(registrationForm.password())))
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid password.").build())
        );

        if (outboundUserRepository.isUsernameExists(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build());
        }

        if (outboundUserRepository.isEmailExists(email)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Email already exists.").build());
        }

        UserAccount userAccount = Result.ofThrowable(() ->
                UserAccount.of(username, email, password)
        ).orElseThrow(
                () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid user account.").build())
        );

        inboundUserRepository.save(userAccount);

        var token = EmailConfirmationToken.createToken(userAccount);
        inboundUserRepository.saveUserToken(token);

        String link = String.format("/token/verification?%s", token.getToken());
        emailInteractionService.sendToEmail(email, link);

        return Response.ok("redirect:/login").build();
    }

    @PATCH @Path("/token/verification")
    public final Response tokenVerification(@QueryParam("token") UUID token) throws IllegalAccessException {
        var foundToken = outboundUserRepository
                .findToken(token)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This token is not exists").build())
                );

        if (foundToken.isExpired()) {
            inboundUserRepository.deleteByToken(foundToken);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token was expired.").build());
        }

        foundToken.confirm();
        foundToken.getUserAccount().enable();
        inboundUserRepository.enable(foundToken);

        return Response.ok("token-verification").build();
    }
}