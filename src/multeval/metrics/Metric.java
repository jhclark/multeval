package multeval.metrics;

import java.util.List;

/**
 * Computes a metric score given that metric's sufficient statistics (Yes, we're
 * skipping the hard, expensive part and letting the metric reference
 * implementations take care of that)
 * 
 * @author jhclark
 * 
 */
public interface Metric {
	public float[] stats(String sentence, List<String> refs);
	
	public double score(double[] suffStats);
}
