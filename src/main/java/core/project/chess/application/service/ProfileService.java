package core.project.chess.application.service;

import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.value_objects.Username;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.ProfilePicture;
import core.project.chess.infrastructure.dal.files.ImageFileRepository;
import core.project.chess.infrastructure.files.StreamUtils;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

import static core.project.chess.application.util.JSONUtilities.responseException;

@ApplicationScoped
public class ProfileService {

    private final StreamUtils streamUtils;

    private final ImageFileRepository imageFileRepository;

    private final OutboundUserRepository outboundUserRepository;

    ProfileService(StreamUtils streamUtils,
                   ImageFileRepository imageFileRepository,
                   OutboundUserRepository outboundUserRepository) {
        this.streamUtils = streamUtils;
        this.imageFileRepository = imageFileRepository;
        this.outboundUserRepository = outboundUserRepository;
    }

    public void putProfilePicture(InputStream inputStream, String username) {
        Username usernameObj;
        try {
            usernameObj = new Username(username);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        final User user = outboundUserRepository
                .findByUsername(usernameObj)
                .orElseThrow(() -> {
                    Log.error("User not found");
                    return responseException(Response.Status.BAD_REQUEST, "User not found.");
                });

        final byte[] picture = streamUtils.toByteArray(inputStream)
                .orElseThrow(() -> {
                    Log.error("Can`t convert input stream into byte array.");
                    return responseException(Response.Status.BAD_REQUEST, "Invalid picture");
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

    public ProfilePicture profilePicture(String username) {
        Username usernameObj;
        try {
            usernameObj = new Username(username);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        final User user = outboundUserRepository.findByUsername(usernameObj)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        return imageFileRepository
                .load(ProfilePicture.profilePicturePath(user.id().toString()))
                .orElseGet(ProfilePicture::defaultProfilePicture);
    }

    public void deleteProfilePicture(String username) {
        Username usernameObj;
        try {
            usernameObj = new Username(username);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        final User user = outboundUserRepository.findByUsername(usernameObj)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "User not found."));

        user.deleteProfilePicture();
        imageFileRepository.delete(ProfilePicture.profilePicturePath(user.id().toString()));
    }
}
