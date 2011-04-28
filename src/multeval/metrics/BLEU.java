package multeval.metrics;

import java.util.ArrayList;
import java.util.List;

import multeval.util.ArrayUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import jannopts.Option;
import jbleu.JBLEU;

// a MultiMetric wrapper around the jBLEU metric
public class BLEU implements Metric {
	
	@Option(shortName = "c", longName = "bleu.closestRefLength", usage = "Use closest reference length when determining brevity penalty? (true behaves like IBM BLEU, false behaves like old NIST BLEU)", defaultValue="true")
	boolean closestRefLength;
	
	private JBLEU bleu = new JBLEU();

	@Override
	public float[] stats(String hyp, List<String> refs) {
		
		List<String> tokHyp = Lists.newArrayList(Splitter.on(' ').split(hyp));
		List<List<String>> tokRefs = new ArrayList<List<String>>();
		for(String ref : refs) {
			tokRefs.add(Lists.newArrayList(Splitter.on(' ').split(ref)));
		}
		
		int[] result = new int[JBLEU.getSuffStatCount()];
		bleu.stats(tokHyp, tokRefs, result);
		return ArrayUtils.toFloatArray(result);
	}

	@Override
	public double score(double[] suffStats) {
		return bleu.score(ArrayUtils.toIntArray(suffStats));
	}
	
	@Override
	public String toString() {
		return "BLEU";
	}
}
