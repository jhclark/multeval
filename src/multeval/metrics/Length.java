package multeval.metrics;

import jannopts.ConfigurationException;
import jannopts.Configurator;

import java.util.List;

import jbleu.JBLEU;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

// a MultiMetric wrapper around the jBLEU metric
public class Length extends Metric<IntStats> {
	
	@Override
	public IntStats stats(String hyp, List<String> refs) {
		
		List<String> hypToks =
				Lists.newArrayList(Splitter.on(CharMatcher.BREAKING_WHITESPACE).split(hyp));
		List<List<String>> tokRefs = BLEU.tokenizeRefs(refs);
		int iRef = JBLEU.pickReference(hypToks, tokRefs);
		
		IntStats result = new IntStats(2);
		result.arr[0] = hypToks.size();
		result.arr[1] = tokRefs.get(iRef).size();
		return result;
	}

	@Override
	public double score(IntStats suffStats) {
		int hypLen = suffStats.arr[0];
		int refLen = suffStats.arr[1];
		return (double) hypLen / (double) refLen;
	}
	
	@Override
	public String toString() {
		return "Length";
	}

	@Override
	public void configure(Configurator opts) throws ConfigurationException {
		opts.configure(this);
	}
}
