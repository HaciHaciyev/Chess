package core.project.chess.domain.user.value_objects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Surname(String surname) {

    public static final int MIN_SIZE = 3;

    public static final int MAX_SIZE = 56;

    private static final String SURNAME_REGEX = "^[A-Za-z]+$";

    private static final Pattern SURNAME_PATTERN = Pattern.compile(SURNAME_REGEX);

    public Surname {
        validate(surname);
    }

    public static void validate(String surname) {
        if (surname == null) {
            throw new IllegalArgumentException("Surname must not be null.");
        }
        if (surname.isBlank()) {
            throw new IllegalArgumentException("Surname should`t be blank.");
        }
        if (surname.length() < MIN_SIZE || surname.length() > MAX_SIZE) {
            throw new IllegalArgumentException("Surname should be greater than 5 characters and smaller than 25.");
        }

        Matcher matcher = SURNAME_PATTERN.matcher(surname);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Surname should match the pattern.");
        }
    }
}
