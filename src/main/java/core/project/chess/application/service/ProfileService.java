package core.project.chess.application.service;

import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.dal.files.ImageFileRepository;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ProfileService {

    private final ImageFileRepository imageFileRepository;

    private final OutboundUserRepository outboundUserRepository;

    ProfileService(ImageFileRepository imageFileRepository, OutboundUserRepository outboundUserRepository) {
        this.imageFileRepository = imageFileRepository;
        this.outboundUserRepository = outboundUserRepository;
    }

    public void putProfilePicture(byte[] picture, Username username) {
        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(() -> {
                    Log.error("User not found");
                    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User not found.").build());
                });

        final ProfilePicture profilePicture = Result
                .ofThrowable(() -> ProfilePicture.of(picture, userAccount))
                .orElseThrow(() -> {
                    String errorMessage = "Invalid image or image size is too big";
                    Log.error(errorMessage);
                    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build());
                });

        Log.info("Successfully validate image.");
        userAccount.setProfilePicture(profilePicture);

        imageFileRepository.put(userAccount);
    }

    public ProfilePicture getProfilePicture(Username username) {
        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User not found.").build())
                );

        return imageFileRepository
                .load(ProfilePicture.profilePicturePath(userAccount.getId().toString()))
                .orElseGet(ProfilePicture::defaultProfilePicture);
    }

    public void deleteProfilePicture(Username username) {
        final UserAccount userAccount = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User not found.").build())
                );

        userAccount.deleteProfilePicture();
        imageFileRepository.delete(ProfilePicture.profilePicturePath(userAccount.getId().toString()));
    }
}
