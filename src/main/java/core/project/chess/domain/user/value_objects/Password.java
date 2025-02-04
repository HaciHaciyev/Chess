package core.project.chess.domain.user.value_objects;

import java.util.Objects;

public record Password(String password) {
    public static final int MIN_SIZE = 8;
    public static final int MAX_SIZE = 64;

    public static boolean validate(String password) {
        if (Objects.isNull(password)) {
            return false;
        }
        if (password.isBlank()) {
            return false;
        }
        if (password.length() < MIN_SIZE) {
            return false;
        }
        return password.length() <= MAX_SIZE;
    }
}
