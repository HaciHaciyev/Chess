package core.project.chess.application.controller;

import core.project.chess.application.dto.user.LoginForm;
import core.project.chess.application.dto.user.RegistrationForm;
import core.project.chess.application.service.UserAccountService;
import core.project.chess.domain.aggregates.chess.value_objects.ChessGameHistory;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.infrastructure.utilities.containers.Result;
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

    private final UserAccountService userAccountService;

    private final OutboundChessRepository outboundChessRepository;

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

    @Authenticated
    @PUT @Path("/put-profile-picture")
    public Response putProfilePicture(byte[] picture) {
        if (Objects.isNull(picture)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Picture can`t be null.").build());
        }

        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        userAccountService.putProfilePicture(picture, username);
        return Response.accepted("Successfully saved picture.").build();
    }

    @Authenticated
    @GET @Path("/profile-picture")
    public Response getProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        return Response.ok(userAccountService.getProfilePicture(username).profilePicture()).build();
    }

    @Authenticated
    @DELETE @Path("/delete-profile-picture")
    public Response deleteProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        userAccountService.deleteProfilePicture(username);
        return Response.accepted("Successfully delete a profile image.").build();
    }

    @Authenticated
    @GET @Path("/game-history")
    public Response gameHistory(@QueryParam("pageNumber") int pageNumber) {
        List<ChessGameHistory> listOfGames = outboundChessRepository
                .listOfGames(new Username(jwt.getName()), pageNumber)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.\uD83D\uDC7B").build())
                );

        return Response.ok(listOfGames).build();
    }

    @Authenticated
    @GET @Path("/partners")
    public Response partners(@QueryParam("pageNumber") int pageNumber) {
        List<String> partnersUsernames = outboundChessRepository
                .listOfPartners(jwt.getName(), pageNumber)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.\uD83D\uDC7B").build())
                );

        return Response.ok(partnersUsernames).build();
    }
}