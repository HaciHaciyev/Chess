package core.project.chess.infrastructure.utilities.containers;

import java.util.Objects;

public final class Triple<F, S, T> {

    private final F first;
    private final S second;
    private final T third;

    private Triple(F first, S second, T third) {
        Objects.requireNonNull(first, "First must not be null");
        Objects.requireNonNull(second, "Second must not be null");
        Objects.requireNonNull(third, "Third must not be null");
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Creates a new {@link Triple} for the given elements.
     *
     * @param first must not be {@literal null}.
     * @param second must not be {@literal null}.
     * @param third must not be {@literal null}.
     * @return
     */
    public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
        return new Triple<>(first, second, third);
    }

    /**
     * Returns the first element of the {@link Triple}.
     *
     * @return
     */
    public F getFirst() {
        return first;
    }

    /**
     * Returns the second element of the {@link Triple}.
     *
     * @return
     */
    public S getSecond() {
        return second;
    }

    /**
     * Returns the third element of the {@link Triple}.
     *
     * @return
     */
    public T getThird() {
        return third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(first, triple.first) && Objects.equals(second, triple.second) && Objects.equals(third, triple.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }

    @Override
    public String toString() {
        return String.format("%s->%s->%s", this.first, this.second, this.third);
    }
}
