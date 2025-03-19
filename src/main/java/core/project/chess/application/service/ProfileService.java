package core.project.chess.application.service;

import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.dal.files.ImageFileRepository;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import static core.project.chess.application.util.JSONUtilities.responseException;

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
                    return responseException(Response.Status.BAD_REQUEST, "User not found.");
                });

        final ProfilePicture profilePicture = Result
                .ofThrowable(() -> ProfilePicture.of(picture, userAccount))
                .orElseThrow(() -> {
                    String errorMessage = "Invalid image or image size is too big";
                    Log.error(errorMessage);
                    return responseException(Response.Status.BAD_REQUEST, errorMessage);
                });

        Log.info("Successfully validate image.");
        userAccount.setProfilePicture(profilePicture);

        imageFileRepository.put(userAccount);
    }

    public ProfilePicture getProfilePicture(Username username) {
        final UserAccount userAccount = outboundUserRepository.findByUsername(username)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        return imageFileRepository
                .load(ProfilePicture.profilePicturePath(userAccount.getId().toString()))
                .orElseGet(ProfilePicture::defaultProfilePicture);
    }

    public void deleteProfilePicture(Username username) {
        final UserAccount userAccount = outboundUserRepository.findByUsername(username)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        userAccount.deleteProfilePicture();
        imageFileRepository.delete(ProfilePicture.profilePicturePath(userAccount.getId().toString()));
    }
}
