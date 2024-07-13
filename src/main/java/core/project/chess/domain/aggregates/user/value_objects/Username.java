package core.project.chess.domain.aggregates.user.value_objects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Username(String username) {

    public Username {
        if (Objects.isNull(username)) {
            // TODO
        }
        if (username.isBlank()) {
            // TODO
        }

        String usernameRegex = "^[a-zA-Z0-9]*$";
        Pattern pattern = Pattern.compile(usernameRegex);
        Matcher matcher = pattern.matcher(username);
        if (!matcher.matches()) {
            // TODO
        }
    }
}
