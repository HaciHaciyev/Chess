package core.project.chess.domain.aggregates.user.value_objects;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public record Email(String email) {

    public Email {
        if (Objects.isNull(email)) {
            log.error("Email can`t be null");
            throw new NullPointerException("Email can`t be null");
        }
        if (email.isBlank()) {
            log.error("Email can`t be blank");
            throw new IllegalArgumentException("Email can`t be blank");
        }

        String emailRegex = "^(\\S+)@(\\S+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            log.error("Email format error");
            throw new IllegalArgumentException("Email format error");
        }
    }

    public String getEmailAddress() {
        return email;
    }
}
