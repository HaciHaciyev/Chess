package core.project.chess.infrastructure.config.security;

import core.project.chess.domain.aggregates.user.value_objects.Password;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.inject.Singleton;

import java.util.Objects;

@Singleton
public class PasswordEncoder {

    private final Argon2 argon2;

    public PasswordEncoder() {
        this.argon2 = Argon2Factory.create();
    }

    public String encode(Password password) {
        Objects.requireNonNull(password);
        return argon2.hash(22, 65536, 1, password.password().toCharArray());
    }

    public boolean verify(Password password, Password hashed) {
        Objects.requireNonNull(password);
        Objects.requireNonNull(hashed);
        return argon2.verify(hashed.password(), password.password().toCharArray());
    }
}