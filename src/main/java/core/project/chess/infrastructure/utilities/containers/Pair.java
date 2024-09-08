package core.project.chess.infrastructure.utilities.containers;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Pair<S, T> {

	private final S first;
	private final T second;

	private Pair(S first, T second) {
		Objects.requireNonNull(first, "First must not be null");
		Objects.requireNonNull(second, "Second must not be null");
		this.first = first;
		this.second = second;
	}

	/**
	 * Creates a new {@link Pair} for the given elements.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @return
	 */
	public static <S, T> Pair<S, T> of(S first, T second) {
		return new Pair<>(first, second);
	}

	/**
	 * Returns the first element of the {@link Pair}.
	 *
	 * @return
	 */
	public S getFirst() {
		return first;
	}

	/**
	 * Returns the second element of the {@link Pair}.
	 *
	 * @return
	 */
	public T getSecond() {
		return second;
	}

	/**
	 * A collector to create a {@link Map} from a {@link Stream} of {@link Pair}s.
	 *
	 * @return
	 */
	public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
		return Collectors.toMap(Pair::getFirst, Pair::getSecond);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
	}

	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}

	@Override
	public String toString() {
		return String.format("%s->%s", this.first, this.second);
	}
}
