package metrics;

/**
 * Computes a metric score given that metric's sufficient statistics (Yes, we're
 * skipping the hard, expensive part and letting the metric reference
 * implementations take care of that)
 * 
 * @author jhclark
 * 
 */
public interface Metric {
	public void stats(String[] sentence, String[][] refs, float[] result);
	
	public double score(double[] suffStats);
}
