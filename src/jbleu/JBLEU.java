package jbleu;

import java.util.List;

import jbleu.util.TrieCursor;

// this is a reimplementation of NIST's MT eval 13b
// minus the problematic pre-processing and SGML handling
public class JBLEU {

	// TODO: Support BLEU other than BLEU=4

	// TODO: Return 1-4-gram precision, length ratio, brevity penalty, and tok
	// OOVs

	public void stats(List<String> hyp, List<List<String>> refs, int[] result) {
		assert result.length == 9;
		assert refs.size() > 0;

		// 1) choose reference length
		int hypLen = hyp.size();
		int refLen = refs.get(0).size();
		// TODO: "Closest" or "least harsh"?
		int curDist = Math.abs(hypLen - refLen);
		for (List<String> ref : refs) {
			// for now, always use closest ref
			int myDist = Math.abs(hypLen - ref.size());
			if (myDist < curDist) {
				refLen = ref.size();
			}
		}

		// TODO: Integer-ify everything? Or is there too much overhead there?

		// 2) determine the bag of n-grams we can score against
		// build a simple trie
		TrieCursor<String, Integer> root = new TrieCursor<String, Integer>(null);
		int N = 4;
		for (List<String> ref : refs) {
			for (int order = 1; order <= N; order++) {
				for (int i = 0; i <= ref.size() - i; i++) {
					root.extend(ref.get(i));
				}
			}
		}

		// 3) now match n-grams
	}

	public float score(int[] stats) {
		return 0.0f;
	}
}
