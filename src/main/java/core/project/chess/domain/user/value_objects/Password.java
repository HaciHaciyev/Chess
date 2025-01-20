package core.project.chess.domain.user.value_objects;

import java.util.Objects;

public record Password(String password) {
    public static final int MIN_SIZE = 8;
    public static final int MAX_SIZE = 64;

    public Password {
        if (Objects.isNull(password)) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (password.length() < MIN_SIZE) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    public static boolean validateMaxSize(String password) {
        return password.length() <= MAX_SIZE;
    }
}
