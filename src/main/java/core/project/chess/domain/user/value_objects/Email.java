package core.project.chess.domain.user.value_objects;

import io.quarkus.logging.Log;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Email(String email) {

    private static final String emailRegex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

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
