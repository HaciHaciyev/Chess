package core.project.chess.domain.aggregates.user.value_objects;

import lombok.Getter;

import java.util.UUID;

@Getter
public final class Token {

    private final UUID token;

    public Token(UUID token) {
        this.token = token;
    }

    public static Token createToken() {
        return new Token(randomToken());
    }

    private static UUID randomToken() {
        return UUID.randomUUID();
    }
}
