package jbleu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import jbleu.util.Ngram;

// this is a reimplementation of NIST's MT eval 13b
// minus the problematic pre-processing and SGML handling
public class JBLEU {
	
	// TODO: Support BLEU other than BLEU=4
	private static final int N = 4;

	// TODO: Return 1-4-gram precision, length ratio, brevity penalty, and tok
	// OOVs

	public void stats(List<String> hyp, List<List<String>> refs, int[] result) {
		assert result.length == 9;
		assert refs.size() > 0;

		// 1) choose reference length
		int hypLen = hyp.size();
		int selectedRefLen = refs.get(0).size();
		// TODO: "Closest" or "least harsh"?
		int curDist = Math.abs(hypLen - selectedRefLen);
		for (List<String> ref : refs) {
			// for now, always use closest ref
			int myDist = Math.abs(hypLen - ref.size());
			if (myDist < curDist) {
				selectedRefLen = ref.size();
			}
		}

		// TODO: Integer-ify everything inside Ngram? Or is there too much overhead there?

		// 2) determine the bag of n-grams we can score against
		// build a simple tries
		Multiset<Ngram> refNgrams = HashMultiset.create();
		for (List<String> ref : refs) {
			for (int order = 1; order <= N; order++) {
				for (int i = 0; i <= ref.size() - order; i++) {
					List<String> toks = ref.subList(i, i+order);
					Ngram ngram = new Ngram(toks);
					refNgrams.add(ngram);
				}
			}
		}

		// 3) now match n-grams
		int[] attempts = new int[N];
		int[] matches = new int[N];
		for (int order = 1; order <= N; order++) {
			for (int i = 0; i <= hyp.size() - order; i++) {
				List<String> toks = hyp.subList(i, i+order);
				Ngram ngram = new Ngram(toks);
				boolean found = refNgrams.remove(ngram);
				++attempts[order-1];
				if(found) {
					++matches[order-1];
				}
			}
		}
		
		// 4) assign sufficient stats
		System.arraycopy(attempts, 0, result, 0, N);
		System.arraycopy(matches, 0, result, N, N);
		result[8] = selectedRefLen;
	}
	
	private static double getAttemptedNgrams(int[] suffStats, int j) {
		return suffStats[j];
	}

	private static double getMatchingNgrams(int[] suffStats, int j) {
		return suffStats[j + N];
	}

	private static double getRefWords(int[] suffStats) {
		return suffStats[N*2];
	}

	
//	###############################################################################################################################
//	# Default method used to compute the BLEU score, using smoothing.
//	# Note that the method used can be overridden using the '--no-smoothing' command-line argument
//	# The smoothing is computed by taking 1 / ( 2^k ), instead of 0, for each precision score whose matching n-gram count is null
//	# k is 1 for the first 'n' value for which the n-gram match count is null
//	# For example, if the text contains:
//	#   - one 2-gram match
//	#   - and (consequently) two 1-gram matches
//	# the n-gram count for each individual precision score would be:
//	#   - n=1  =>  prec_count = 2     (two unigrams)
//	#   - n=2  =>  prec_count = 1     (one bigram)
//	#   - n=3  =>  prec_count = 1/2   (no trigram,  taking 'smoothed' value of 1 / ( 2^k ), with k=1)
//	#   - n=4  =>  prec_count = 1/4   (no fourgram, taking 'smoothed' value of 1 / ( 2^k ), with k=2)
//	###############################################################################################################################
// segment-level bleu smoothing is done by default and is similar to that of bleu-1.04.pl (IBM)
	public double score(int[] suffStats) {
		Preconditions.checkArgument(suffStats.length == N*2+1, "BLEU sufficient stats must be of length N*2+1");

		final double brevityPenalty;
		double refWords = getRefWords(suffStats);
		double hypWords = getAttemptedNgrams(suffStats, 0);
		if(hypWords < refWords) {
			brevityPenalty = Math.exp(1.0 - refWords / hypWords);
		} else {
			brevityPenalty = 1.0;
		}
		assert brevityPenalty >= 0.0;
		assert brevityPenalty <= 1.0;
		
		double score = 0.0;
		double smooth = 1.0;

		for (int j = 0; j < N; j++) {
			double attemptedNgramsJ = getAttemptedNgrams(suffStats, j);
			double matchingNgramsJ = getMatchingNgrams(suffStats, j);
			final double iscore;
			if (attemptedNgramsJ == 0) {
				iscore = 0.0;
			} else if (matchingNgramsJ == 0) {
				smooth *= 2;
				double smoothedPrecision = 1.0 / (smooth * attemptedNgramsJ);
				iscore = Math.log(smoothedPrecision);
			} else {
				double precisionAtJ = matchingNgramsJ / attemptedNgramsJ;
				iscore = Math.log(precisionAtJ);
			}
			// TODO: Allow non-uniform weights instead of just the "baseline" 1/4 from Papenini			
			double ngramOrderWeight = 0.25;
			score += iscore * ngramOrderWeight;
			
			assert Math.exp(iscore * ngramOrderWeight) <= 1.0 : String.format("ERROR for order %d-grams iscore: %f -> %f :: %s", j+1, iscore, Math.exp(iscore * ngramOrderWeight), Arrays.toString(suffStats));
			assert Math.exp(score * ngramOrderWeight) <= 1.0;
		}

		double totalScore = brevityPenalty * Math.exp(score);
		
		if (totalScore > 1.0) {
			System.err.println("BLEU: Thresholding out of range score: " + totalScore + "; stats: " + Arrays.toString(suffStats));
			totalScore = 1.0;
		} else if (totalScore < 0.0) {
			System.err.println("BLEU: Thresholding out of range score: " + totalScore);
			totalScore = 0.0;
		}
		return totalScore;
	}
	
	public static int getSuffStatCount() {
		// attempted 1-4 gram counts
		// matching 1-4 gram counts
		// length of selected reference for brevity penalty
		return N * 2 + 1;
	}
	
	public static void main(String[] args) {
		JBLEU bleu = new JBLEU();
		
		int[] result = new int[JBLEU.getSuffStatCount()];
		
		List<String> hyp = Lists.newArrayList(Splitter.on(' ').split("This is a hypothesis sentence ."));
		List<List<String>> refs = new ArrayList<List<String>>();
		refs.add(Lists.newArrayList(Splitter.on(' ').split("This is the first reference sentence .")));
		refs.add(Lists.newArrayList(Splitter.on(' ').split("This is the second reference sentence .")));
		
		bleu.stats(hyp, refs, result);
		
		System.err.println(Arrays.toString(result));
		System.err.println(bleu.score(result));
	}
}
