package core.project.chess.domain.aggregates.user.value_objects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Username(String username) {
    //TODO почему тут пробел в regEX?
    private static final String usernameRegex = "^[a-zA-Z0-9 ]*$";

    private static final Pattern pattern = Pattern.compile(usernameRegex);

    //TODO нет ограничения на длину имени
    public Username {
        if (Objects.isNull(username)) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        Matcher matcher = pattern.matcher(username);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }
    }
}
