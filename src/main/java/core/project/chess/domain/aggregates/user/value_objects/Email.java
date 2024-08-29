package core.project.chess.domain.aggregates.user.value_objects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Email(String email) {

    public Email {
        if (Objects.isNull(email)) {
            throw new NullPointerException("Email can`t be null");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email can`t be blank");
        }

        String emailRegex = "^(\\S+)@(\\S+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Email format error");
        }
    }
}
