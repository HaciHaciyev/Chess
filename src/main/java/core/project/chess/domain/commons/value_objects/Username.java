package core.project.chess.domain.commons.value_objects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Username(String username) {

    private static final String usernameRegex = "^[a-zA-Z0-9]*$";

    private static final Pattern pattern = Pattern.compile(usernameRegex);

    public Username {
        validate(username);
    }

    public static void validate(String username) {
        if (Objects.isNull(username)) {
            throw new IllegalArgumentException("Username cannot be null.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        if (username.length() > 24) {
            throw new IllegalArgumentException("This username is too long.");
        }

        Matcher matcher = pattern.matcher(username);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Username contains invalid characters.");
        }
    }

    public static boolean isValid(String username) {
        if (Objects.isNull(username)) {
            return false;
        }
        if (username.isBlank()) {
            return false;
        }

        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }
}
