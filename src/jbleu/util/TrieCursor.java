package jbleu.util;

import java.util.HashMap;
import java.util.Map;

public static class TrieCursor<T, V> {
	public final V value;
	private Map<T, TrieCursor<T, V>> next;

	public TrieCursor(V value) {
		this.value = value;
	}

	// null if not found
	public TrieCursor<T, V> match(T ext) {
		if (next == null) {
			return null;
		} else {
			return next.get(ext);
		}
	}

	protected TrieCursor<T, V> extend(T ext) {
		if (next == null) {
			next = new HashMap<T, TrieCursor<T, V>>();
		}
		TrieCursor<T, V> match = match(ext);
		if (match == null) {
			match = new TrieCursor<T, V>(ext);
			next.put(ext, match);
		}
		return match;
	}

	public String toString() {
		return value.toString();
	}

}
