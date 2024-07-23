package core.project.chess.infrastructure.utilities;

import java.util.NoSuchElementException;
import java.util.Objects;

public class StatusPair<T> {
    private final boolean status;
    private final T value;

    private StatusPair(boolean status, T value) {
        this.status = status;
        this.value = value;
    }

    public static <T> StatusPair<T> ofTrue(T value) {
        Objects.requireNonNull(value);
        return new StatusPair<>(true, value);
    }

    public static <T> StatusPair<T> ofFalse() {
        return new StatusPair<>(false, null);
    }

    public boolean status() {
        return status;
    }

    public T valueOrElse() {
        if (value == null) {
            throw new NoSuchElementException();
        }

        return value;
    }
}