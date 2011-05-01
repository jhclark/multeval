package multeval.metrics;

import edu.cmu.meteor.scorer.MeteorStats;

public class METEORStats extends SuffStats<METEORStats> {

	public final MeteorStats meteorStats;

	public METEORStats(MeteorStats other) {
		this.meteorStats = other;
	}

	private METEORStats() {
		this.meteorStats = new MeteorStats();
	}

	@Override
	public void add(METEORStats other) {
		// "exact" meteor score isn't strictly a sum
		// due to a corner case in the fragmentation penalty
		this.meteorStats.addStats(other.meteorStats);
	}

	@Override
	public SuffStats<METEORStats> create() {
		return new METEORStats();
	}
	
	@Override
	public String toString() {
		return meteorStats.toString();
	}
}
