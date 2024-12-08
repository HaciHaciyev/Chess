package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.UserAccountService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Objects;

@PermitAll
@Path("/account")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserController {

    private final JsonWebToken jwt;

    private final UserAccountService userAccountService;

    @POST @Path("/login")
    public Response login(LoginForm loginForm) {
        if (Objects.isNull(loginForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Login form is null.").build());
        }

        return Response.ok(userAccountService.login(loginForm)).build();
    }

    @POST @Path("/registration")
    public Response registration(RegistrationForm registrationForm) {
        if (Objects.isNull(registrationForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Registration form is null.").build());
        }

        userAccountService.registration(registrationForm);
        return Response.ok("Registration successful. Verify you email.").build();
    }

    @PATCH @Path("/token/verification")
    public Response tokenVerification(@QueryParam("token") String token) {
        if (Objects.isNull(token)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token can`t be null.").build());
        }

        userAccountService.tokenVerification(token);
        return Response.ok("Now, account is enabled.").build();
    }

    @PATCH @Path("/refresh-token")
    public Response refresh(@HeaderParam("Refresh-Token") String refreshToken) {
        if (Objects.isNull(refreshToken) || refreshToken.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid refresh token.").build());
        }

        return Response.ok(userAccountService.refreshToken(refreshToken)).build();
    }
}