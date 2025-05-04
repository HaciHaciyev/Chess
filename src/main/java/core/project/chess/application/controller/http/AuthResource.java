package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.AuthService;
import core.project.chess.infrastructure.telemetry.TelemetryService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

import static core.project.chess.application.util.JSONUtilities.responseException;

@PermitAll
@Path("/account")
public class AuthResource {

    private final AuthService authService;

    private final TelemetryService telemetry;

    private static final AttributeKey<String> USERNAME = AttributeKey.stringKey("chessland.http.registration.username");

    AuthResource(AuthService authService, TelemetryService telemetry) {
        this.authService = authService;
        this.telemetry = telemetry;
    }

    @POST
    @Path("/registration")
    @WithSpan("REGISTRATION RESOURCE")
    public Response registration(RegistrationForm form) {
        if (Objects.isNull(form))
            throw responseException(Response.Status.BAD_REQUEST, "Registration form is null.");

        telemetry.setSpanAttribute(USERNAME, form.username());

        authService.registration(form);
        return Response.ok("Registration successful. Verify your email.").build();
    }

    @GET
    @Path("/resend/verification/token")
    @WithSpan("RESEND OTP RESOURCE")
    public Response resend(@QueryParam("email") String email) {
        if (Objects.isNull(email))
            throw responseException(Response.Status.BAD_REQUEST, "Email us null");

        authService.resendVerificationToken(email);
        return Response.ok().build();
    }

    @PATCH
    @Path("/token/verification")
    @WithSpan("EMAIL CONFIRMATION RESOURCE")
    public Response verification(@QueryParam("token") String token) {
        if (Objects.isNull(token))
            throw responseException(Response.Status.BAD_REQUEST, "Token is null.");

        authService.verification(token);
        return Response.ok("Now, account is enabled.").build();
    }

    @POST
    @Path("/login")
    @WithSpan("LOGIN RESOURCE")
    public Response login(LoginForm loginForm) {
        if (Objects.isNull(loginForm))
            throw responseException(Response.Status.BAD_REQUEST, "Login form is null.");

        telemetry.setSpanAttribute(USERNAME, loginForm.username());
        return Response.ok(authService.login(loginForm)).build();
    }

    @PATCH
    @Path("/refresh-token")
    @WithSpan("REFRESH TOKEN RESOURCE")
    public Response refresh(@HeaderParam("Refresh-Token") String refreshToken) {
        if (Objects.isNull(refreshToken) || refreshToken.isBlank())
            throw responseException(Response.Status.BAD_REQUEST, "Refresh token is null.");

        return Response.ok(authService.refreshToken(refreshToken)).build();
    }
}