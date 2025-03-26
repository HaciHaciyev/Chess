package core.project.chess.domain.user.value_objects;

import java.util.Objects;

public record Password(String password) {
    public static final int MIN_SIZE = 8;
    public static final int MAX_SIZE = 64;
    public static final String INVALID_PASSWORD = "Invalid password.";

    public static void validate(String password) {
        if (Objects.isNull(password)) {
            throw new IllegalArgumentException(INVALID_PASSWORD);
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException(INVALID_PASSWORD);
        }
        if (password.length() < MIN_SIZE) {
            throw new IllegalArgumentException(INVALID_PASSWORD);
        }
        if (password.length() > MAX_SIZE) {
            throw new IllegalArgumentException(INVALID_PASSWORD);
        }
    }
}
