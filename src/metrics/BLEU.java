package metrics;

import java.util.List;

import jannopts.Option;

public class BLEU implements Metric {
	
	@Option(shortName = "c", longName = "bleu.closestRefLength", usage = "Use closest reference length when determining brevity penalty? (true behaves like IBM BLEU, false behaves like old NIST BLEU)", defaultValue="true")
	boolean closestRefLength;

	@Override
	public float[] stats(String hyps, List<String> refs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double score(double[] suffStats) {
		// TODO Auto-generated method stub
		return 0;
	}
}
