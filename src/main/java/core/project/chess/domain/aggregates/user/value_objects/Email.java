package core.project.chess.domain.aggregates.user.value_objects;

import io.quarkus.logging.Log;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Email(String email) {

    private static final String emailRegex = "^(\\S+)@(\\S+)$";

    private static final Pattern pattern = Pattern.compile(emailRegex);

    public Email {
        if (Objects.isNull(email)) {
            throw new NullPointerException("Email can`t be null");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email can`t be blank");
        }

        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            Log.errorf("Invalid email format: %s", email);
            throw new IllegalArgumentException("Email format error");
        }
    }
}
