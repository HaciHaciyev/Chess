package core.project.chess.domain.aggregates.user.value_objects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Email(String email) {

    public Email {
        if (email == null) {
            // TODO
        }
        if (email.isBlank()) {
            // TODO
        }

        String emailRegex = "^(\\S+)@(\\S+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            // TODO
        }
    }
}
