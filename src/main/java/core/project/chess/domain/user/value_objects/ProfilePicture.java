package core.project.chess.domain.user.value_objects;

import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.util.PictureUtility;
import io.quarkus.logging.Log;

import java.util.Arrays;
import java.util.Objects;

public final class ProfilePicture {
    private final String path;
    private final String imageType;
    private final byte[] profilePicture;
    private static final int MAX_SIZE = 2_097_152;
    private static final String PATH_FORMAT = "src/main/resources/static/profile/photos/%s";
    private static final String DEFAULT_PROFILE_PICTURE_PATH = "src/main/resources/static/profile/photos/default-profile-picture.png";

    private ProfilePicture(String path, byte[] profilePicture, String imageType) {
        this.path = path;
        this.profilePicture = profilePicture;
        this.imageType = imageType;
    }

    public static ProfilePicture of(byte[] profilePicture, User user) {
        Objects.requireNonNull(profilePicture, "Profile picture cannot be null");
        Objects.requireNonNull(user, "User account cannot be null");

        String path = profilePicturePath(user.id().toString());
        StatusPair<String> typeOfImage = validate(path, profilePicture);

        if (!typeOfImage.status()) {
            Log.errorf("Invalid profile picture type: %s", typeOfImage.status());
            throw new IllegalArgumentException(String.format("Invalid profile picture type: %s", typeOfImage.status()));
        }

        return new ProfilePicture(path, profilePicture, typeOfImage.orElseThrow());
    }

    public static ProfilePicture fromRepository(String path, byte[] profilePicture) {
        return new ProfilePicture(path, profilePicture, PictureUtility.validateImage(profilePicture).orElseThrow());
    }

    public static ProfilePicture defaultProfilePicture() {
        return new ProfilePicture(DEFAULT_PROFILE_PICTURE_PATH, PictureUtility.defaultImage(), ".png");
    }

    public static String profilePicturePath(String id) {
        return String.format(PATH_FORMAT, id);
    }

    public String path() {
        return path;
    }

    public byte[] profilePicture() {
        byte[] copy = new byte[profilePicture.length];
        System.arraycopy(profilePicture, 0, copy, 0, profilePicture.length);
        return copy;
    }

    public String imageType() {
        return imageType;
    }

    private static StatusPair<String> validate(String path, byte[] profilePicture) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(profilePicture);

        if (profilePicture.length > MAX_SIZE) {
            Log.error("Profile picture is too big.");
            return StatusPair.ofFalse();
        }

        return PictureUtility.validateImage(profilePicture);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProfilePicture that)) return false;

        return Objects.equals(path, that.path) && Objects.equals(imageType, that.imageType) && Arrays.equals(profilePicture, that.profilePicture);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(path);
        result = 31 * result + Objects.hashCode(imageType);
        result = 31 * result + Arrays.hashCode(profilePicture);
        return result;
    }

    @Override
    public String toString() {
        return String.format("""
                {
                Path: %s,
                ProfilePicture: %s,
                File extension: %s
                }
                """, this.path, Arrays.toString(this.profilePicture), this.imageType);
    }
}
