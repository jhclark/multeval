package metrics;

import jannopts.Option;

public class BLEU implements Metric {
	
	@Option(shortName = "c", longName = "bleu.closestRefLength", usage = "Use closest reference length when determining brevity penalty?", defaultValue="true")
	boolean closestRefLength;

	@Override
	public float stats(String[] sentence, String[][] refs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double score(double[] suffStats) {
		// TODO Auto-generated method stub
		return 0;
	}
}
