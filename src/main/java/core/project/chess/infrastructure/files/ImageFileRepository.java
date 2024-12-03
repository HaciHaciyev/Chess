package core.project.chess.infrastructure.files;

import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.ProfilePicture;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@ApplicationScoped
public class ImageFileRepository {

    public void put(UserAccount userAccount) {
        final ProfilePicture profilePicture = userAccount.getProfilePicture();
        final String path = profilePicture.path();
        final byte[] picture = profilePicture.profilePicture();

        try {
            final Path profilePicturePath = Paths.get(path);
            Files.write(profilePicturePath, picture);
            Log.info("Successfully write a/in file");
        } catch (IOException e) {
            Log.errorf("Something get wrong when attempting to put an image: %s", e.getMessage());
        }
    }

    public Optional<ProfilePicture> load(String path) {
        try {
            final Path profilePicturePath = Paths.get(path);

            final boolean isFileExists = Files.exists(profilePicturePath);
            if (isFileExists) {
                byte[] bytes = Files.readAllBytes(profilePicturePath);

                Log.info("Successfully loaded profile picture");
                return Optional.of(ProfilePicture.fromRepository(path, bytes));
            }
        } catch (IOException e) {
            Log.errorf("Can`t load a picture: %s", e.getMessage());
        }

        Log.info("Do not found picture");
        return Optional.empty();
    }

    public void delete(String path) {
        try {
            final Path profilePicturePath = Paths.get(path);

            final boolean isFileExists = Files.exists(profilePicturePath);
            if (isFileExists) {
                Files.delete(profilePicturePath);
                Log.info("Deleted profile picture");
                return;
            }

            Log.info("Can`t find profile picture");
        } catch (IOException e) {
            Log.errorf("Something get wrong in deletion of image file: %s", e.getMessage());
        }
    }
}
