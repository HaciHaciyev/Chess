package core.project.chess.application.service;

import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.infrastructure.dal.files.ImageFileRepository;
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

    public void putProfilePicture(byte[] picture, String username) {
        final User user = outboundUserRepository
                .findByUsername(username)
                .orElseThrow(() -> {
                    Log.error("User not found");
                    return responseException(Response.Status.BAD_REQUEST, "User not found.");
                });

        final ProfilePicture profilePicture = Result
                .ofThrowable(() -> ProfilePicture.of(picture, user))
                .orElseThrow(() -> {
                    String errorMessage = "Invalid image or image size is too big";
                    Log.error(errorMessage);
                    return responseException(Response.Status.BAD_REQUEST, errorMessage);
                });

        Log.info("Successfully validate image.");
        user.setProfilePicture(profilePicture);

        imageFileRepository.put(user);
    }

    public ProfilePicture getProfilePicture(String username) {
        final User user = outboundUserRepository.findByUsername(username)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        return imageFileRepository
                .load(ProfilePicture.profilePicturePath(user.id().toString()))
                .orElseGet(ProfilePicture::defaultProfilePicture);
    }

    public void deleteProfilePicture(String username) {
        final User user = outboundUserRepository.findByUsername(username)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        user.deleteProfilePicture();
        imageFileRepository.delete(ProfilePicture.profilePicturePath(user.id().toString()));
    }
}
