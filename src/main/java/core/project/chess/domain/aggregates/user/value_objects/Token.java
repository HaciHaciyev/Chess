package core.project.chess.domain.aggregates.user.value_objects;

import java.util.Objects;
import java.util.UUID;

public record Token(UUID token) {

    public Token {
        Objects.requireNonNull(token);
    }

    public static Token createToken() {
        return new Token(randomToken());
    }

    private static UUID randomToken() {
        return UUID.randomUUID();
    }
}
