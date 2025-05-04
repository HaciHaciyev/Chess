package core.project.chess.infrastructure.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.opentelemetry.api.trace.Span;
import jakarta.inject.Singleton;

import java.util.Objects;

@Singleton
public class PasswordEncoder {

    private final Argon2 argon2;

    public PasswordEncoder() {
        this.argon2 = Argon2Factory.create();
    }

    public String encode(String password) {
        Span.current().addEvent("Encoding password");
        Objects.requireNonNull(password);
        return argon2.hash(1, 65536, 4, password.toCharArray());
    }

    public boolean verify(String password, String hashed) {
        Span.current().addEvent("Password verification");
        Objects.requireNonNull(password);
        Objects.requireNonNull(hashed);
        return argon2.verify(hashed, password.toCharArray());
    }
}
