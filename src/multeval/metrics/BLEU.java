package multeval.metrics;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;

import java.util.ArrayList;
import java.util.List;

import jbleu.JBLEU;
import multeval.util.LibUtil;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

// a MultiMetric wrapper around the jBLEU metric
public class BLEU extends Metric<IntStats> {
	
	@Option(shortName = "c", longName = "bleu.closestRefLength", usage = "Use closest reference length when determining brevity penalty? (true behaves like IBM BLEU, false behaves like old NIST BLEU)", defaultValue="true")
	boolean closestRefLength;
	
	private JBLEU bleu = new JBLEU();

	@Override
	public IntStats stats(String hyp, List<String> refs) {
		
		List<String> tokHyp = Lists.newArrayList(Splitter.on(' ').split(hyp));
		List<List<String>> tokRefs = new ArrayList<List<String>>();
		for(String ref : refs) {
			tokRefs.add(Lists.newArrayList(Splitter.on(' ').split(ref)));
		}
		
		IntStats result = new IntStats(JBLEU.getSuffStatCount());
		bleu.stats(tokHyp, tokRefs, result.arr);
		return result;
	}

	@Override
	public double score(IntStats suffStats) {
		return bleu.score(suffStats.arr) * 100;
	}
	
	@Override
	public String toString() {
		return "BLEU";
	}

	@Override
	public void configure(Configurator opts) throws ConfigurationException {
		LibUtil.checkLibrary("jbleu.JBLEU", "jBLEU");
		opts.configure(this);
	}
}
