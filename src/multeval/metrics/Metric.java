package multeval.metrics;

import jannopts.ConfigurationException;
import jannopts.Configurator;

import java.util.List;

/**
 * Computes a metric score given that metric's sufficient statistics (Yes, we're
 * skipping the hard, expensive part and letting the metric reference
 * implementations take care of that)
 * 
 * @author jhclark
 * 
 */
public abstract class Metric<Stats extends SuffStats<Stats>> {
	
	public abstract Stats stats(String sentence, List<String> refs);
	
	public abstract double score(Stats suffStats);
	
	// hack around generics by erasure
	@SuppressWarnings("unchecked")
	public double scoreStats(SuffStats<?> suffStats) {
		return score((Stats)suffStats);
	}

	public abstract void configure(Configurator opts) throws ConfigurationException;
}
