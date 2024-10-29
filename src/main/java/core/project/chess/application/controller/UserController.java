package core.project.chess.application.controller;

import core.project.chess.application.dto.gamesession.ChessGameHistory;
import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.UserService;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Objects;

@PermitAll
@Path("/account")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserController {

    private final JsonWebToken jwt;

    private final UserService userService;

    private final OutboundChessRepository outboundChessRepository;

    @POST @Path("/login")
    public final Response login(LoginForm loginForm) {
        if (Objects.isNull(loginForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Login form is null.").build());
        }

        return Response.ok(userService.login(loginForm)).build();
    }

    @POST @Path("/registration")
    public final Response registration(RegistrationForm registrationForm) {
        if (Objects.isNull(registrationForm)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Registration form is null.").build());
        }

        userService.registration(registrationForm);
        return Response.ok("Registration successful. Verify you email.").build();
    }

    @PATCH @Path("/token/verification")
    public final Response tokenVerification(@QueryParam("token") String token) {
        if (Objects.isNull(token)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Token can`t be null.").build());
        }

        userService.tokenVerification(token);
        return Response.ok("Now, account is enabled.").build();
    }

    @PATCH @Path("/refresh-token")
    public final Response refresh(@HeaderParam("Refresh-Token") String refreshToken) {
        if (Objects.isNull(refreshToken) || refreshToken.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid refresh token.").build());
        }

        return Response.ok(userService.refreshToken(refreshToken)).build();
    }

    @Authenticated
    @GET @Path("/game-history")
    public final Response gameHistory(@QueryParam("pageNumber") int pageNumber) {
        List<ChessGameHistory> listOfGames = outboundChessRepository
                .listOfGames(new Username(jwt.getName()), pageNumber)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.\uD83D\uDC7B").build())
                );

        return Response.ok(listOfGames).build();
    }
}