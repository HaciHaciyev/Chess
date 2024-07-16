package core.project.chess.domain.aggregates.user.value_objects;

import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class Token {

    private final int token;

    private Token(int token) {
        this.token = token;
    }

    public static Token createToken() {
        return new Token(randomToken());
    }

    private static int randomToken() {
        return ThreadLocalRandom.current().nextInt(100000, 1000000);
    }
}
