package metrics;

import jannopts.Option;

public class BLEU implements Metric {
	
	@Option(shortName = "c", longName = "bleu.closestRefLength", usage = "Use closest reference length when determining brevity penalty?", defaultValue="true")
	boolean closestRefLength;

	@Override
	public void stats(String[] sentence, String[][] refs, float[] result) {
		// TODO Auto-generated method stub
	}

	@Override
	public double score(double[] suffStats) {
		// TODO Auto-generated method stub
		return 0;
	}
}
