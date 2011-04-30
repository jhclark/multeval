package multeval.metrics;

public abstract class SuffStats<T> {
	public abstract void add(T other);
	
	public abstract SuffStats<T> create();
	
	// hack around generics by erasure
	@SuppressWarnings("unchecked")
	public void add(SuffStats<?> other) {
		add((T) other);
	}
}
