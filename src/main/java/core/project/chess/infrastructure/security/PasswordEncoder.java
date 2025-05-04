package core.project.chess.infrastructure.security;

import core.project.chess.infrastructure.telemetry.TelemetryService;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.inject.Singleton;

import java.util.Objects;

@Singleton
public class PasswordEncoder {

    private final Argon2 argon2;

    private final TelemetryService telemetry;

    public PasswordEncoder(TelemetryService telemetry) {
        this.telemetry = telemetry;
        this.argon2 = Argon2Factory.create();
    }

    public String encode(String password) {
        telemetry.addEvent("Encoding password");
        Objects.requireNonNull(password);
        return argon2.hash(1, 65536, 4, password.toCharArray());
    }

    public boolean verify(String password, String hashed) {
        telemetry.addEvent("Password verification");
        Objects.requireNonNull(password);
        Objects.requireNonNull(hashed);
        return argon2.verify(hashed, password.toCharArray());
    }
}
