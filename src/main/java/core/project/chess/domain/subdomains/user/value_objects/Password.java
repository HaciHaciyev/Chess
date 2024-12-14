package core.project.chess.domain.subdomains.user.value_objects;

import java.util.Objects;

public record Password(String password) {

    public Password {
        if (Objects.isNull(password)) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
}
