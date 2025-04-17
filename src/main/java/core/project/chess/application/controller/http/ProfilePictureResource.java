package core.project.chess.application.controller.http;

import core.project.chess.application.service.ProfileService;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static core.project.chess.application.util.JSONUtilities.responseException;

@Authenticated
@Path("/account")
public class ProfilePictureResource {

    private final JsonWebToken jwt;

    private final ProfileService profileService;

    ProfilePictureResource(Instance<JsonWebToken> jwt, ProfileService profileService) {
        this.jwt = jwt.get();
        this.profileService = profileService;
    }

    @PUT
    @Path("/put-profile-picture")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response putProfilePicture(InputStream inputStream) {
        if (Objects.isNull(inputStream)) {
            throw responseException(Response.Status.BAD_REQUEST, "Picture can`t be null.");
        }

        String username = getUsername();

        try {
            profileService.putProfilePicture(IOUtils.toByteArray(inputStream), username);
            inputStream.close();
        } catch (IOException e) {
            Log.errorf("Can`t read input stream for profile inputStream as bytes: %s", e.getMessage());
            throw responseException(Response.Status.BAD_REQUEST, "Invalid file: corrupted or too big.");
        }
        return Response.accepted("Successfully saved the profile picture for " + username).build();
    }

    @GET
    @Path("/profile-picture")
    public Response getProfilePicture() {
        String username = getUsername();

        ProfilePicture profilePicture = profileService.getProfilePicture(username);
        return Response.ok(Map.of("profilePicture", profilePicture.profilePicture(), "imageType", profilePicture.imageType())).build();
    }

    @DELETE
    @Path("/delete-profile-picture")
    public Response deleteProfilePicture() {
        String username = getUsername();

        profileService.deleteProfilePicture(username);
        return Response.accepted("Successfully deleted the profile image.").build();
    }

    private String getUsername() {
        String username = jwt.getName();
        if (!Username.isValid(username)) {
            throw responseException(Response.Status.BAD_REQUEST, "Invalid username.");
        }

        return username;
    }
}
