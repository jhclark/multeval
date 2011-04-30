package multeval.metrics;

import edu.cmu.meteor.scorer.MeteorStats;

public class MStats extends SuffStats<MStats> {
	
	public final MeteorStats meteorStats;
	
	public MStats(MeteorStats other) {
		this.meteorStats = other;
	}
	
	private MStats() {
		this.meteorStats = new MeteorStats();
	}

	@Override
	public void add(MStats other) {
		this.meteorStats.addStats(other.meteorStats);
	}

	@Override
	public SuffStats<MStats> create() {
		return new MStats();
	}
}
