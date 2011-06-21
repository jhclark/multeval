package multeval.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class CollectionUtils {

	// rom
	// http://philippeadjiman.com/blog/2010/02/20/a-generic-method-for-sorting-google-collections-multiset-per-entry-count/
	public static <T> List<Entry<T>> sortByCount(Multiset<T> multiset) {

		Comparator<Multiset.Entry<T>> occurence_comparator = new Comparator<Multiset.Entry<T>>() {
			public int compare(Multiset.Entry<T> e1, Multiset.Entry<T> e2) {
				return e2.getCount() - e1.getCount();
			}
		};
		List<Entry<T>> sortedByCount = new ArrayList<Entry<T>>(multiset.entrySet());
		Collections.sort(sortedByCount, occurence_comparator);

		return sortedByCount;
	}

	public static <T> List<T> head(List<T> list, int n) {
		if (n <= list.size()) {
			return list.subList(0, n);
		} else {
			return list;
		}
	}
}
