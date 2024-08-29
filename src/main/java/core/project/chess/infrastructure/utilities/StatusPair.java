package core.project.chess.infrastructure.utilities;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

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

    public T orElseThrow() {
        if (value == null) {
            throw new NoSuchElementException();
        }

        return value;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (!this.status) {
            throw exceptionSupplier.get();
        } else {
            return this.value;
        }
    }

    public T valueOrElse(T defaultValue) {
        return value != null ? value : defaultValue;
    }
}