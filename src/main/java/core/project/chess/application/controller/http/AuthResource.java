package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.UserAuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

import static core.project.chess.application.util.JSONUtilities.responseException;

@PermitAll
@Path("/account")
public class AuthResource {

    private final UserAuthService userAuthService;

    AuthResource(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @POST
    @Path("/login")
    public Response login(LoginForm loginForm) {
        if (Objects.isNull(loginForm)) {
            throw responseException(Response.Status.BAD_REQUEST, "Login form is null.");
        }

        return Response.ok(userAuthService.login(loginForm)).build();
    }

    @POST
    @Path("/registration")
    public Response registration(RegistrationForm registrationForm) {
        if (Objects.isNull(registrationForm)) {
            throw responseException(Response.Status.BAD_REQUEST, "Registration form is null.");
        }

        userAuthService.registration(registrationForm);
        return Response.ok("Registration successful. Verify your email.").build();
    }

    @PATCH
    @Path("/token/verification")
    public Response tokenVerification(@QueryParam("token") String token) {
        if (Objects.isNull(token)) {
            throw responseException(Response.Status.BAD_REQUEST, "Token is null.");
        }

        userAuthService.tokenVerification(token);
        return Response.ok("Now, account is enabled.").build();
    }

    @PATCH
    @Path("/refresh-token")
    public Response refresh(@HeaderParam("Refresh-Token") String refreshToken) {
        if (Objects.isNull(refreshToken) || refreshToken.isBlank()) {
            throw responseException(Response.Status.BAD_REQUEST, "Refresh token is null.");
        }

        return Response.ok(userAuthService.refreshToken(refreshToken)).build();
    }
}