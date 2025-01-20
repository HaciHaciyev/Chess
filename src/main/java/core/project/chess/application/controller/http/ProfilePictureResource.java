package core.project.chess.application.controller.http;

import core.project.chess.application.service.ProfileService;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

@Authenticated
@Path("/account")
public class ProfilePictureResource {

    private final JsonWebToken jwt;

    private final ProfileService profileService;

    ProfilePictureResource(JsonWebToken jwt,  ProfileService profileService) {
        this.jwt = jwt;
        this.profileService = profileService;
    }

    @PUT
    @Path("/put-profile-picture")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response putProfilePicture(InputStream inputStream) {
        if (Objects.isNull(inputStream)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Picture can`t be null.").build());
        }

        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        try {
            profileService.putProfilePicture(IOUtils.toByteArray(inputStream), username);
            inputStream.close();
        } catch (IOException e) {
            Log.errorf("Can`t read input stream for profile inputStream as bytes: %s", e.getMessage());
            throw new WebApplicationException("Invalid file");
        }
        return Response.accepted("Successfully saved inputStream.").build();
    }

    @GET
    @Path("/profile-picture")
    public Response getProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        ProfilePicture profilePicture = profileService.getProfilePicture(username);
        return Response.ok(Map.of("profilePicture", profilePicture.profilePicture(), "imageType", profilePicture.imageType())).build();
    }

    @DELETE
    @Path("/delete-profile-picture")
    public Response deleteProfilePicture() {
        Username username = Result
                .ofThrowable(() -> new Username(jwt.getName()))
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build())
                );

        profileService.deleteProfilePicture(username);
        return Response.accepted("Successfully delete a profile image.").build();
    }
}
