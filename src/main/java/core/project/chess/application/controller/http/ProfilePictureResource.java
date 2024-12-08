package core.project.chess.application.http;

import core.project.chess.application.service.UserAccountService;
import core.project.chess.domain.aggregates.user.value_objects.ProfilePicture;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.Objects;

@Authenticated
@Path("/account")
public class ProfilePictureResource {

    private final JsonWebToken jwt;

    private final UserAccountService userAccountService;

    ProfilePictureResource(JsonWebToken jwt, UserAccountService userAccountService) {
        this.jwt = jwt;
        this.userAccountService = userAccountService;
    }

    @PUT
    @Path("/put-profile-picture")
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

    @GET
    @Path("/profile-picture")
    public Response getProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        ProfilePicture profilePicture = userAccountService.getProfilePicture(username);
        return Response.ok(Map.of(profilePicture.profilePicture(), profilePicture.imageType())).build();
    }

    @DELETE
    @Path("/delete-profile-picture")
    public Response deleteProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        userAccountService.deleteProfilePicture(username);
        return Response.accepted("Successfully delete a profile image.").build();
    }
}
