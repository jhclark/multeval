package multeval.util;

import java.util.List;

import multeval.metrics.SuffStats;

import com.google.common.base.Preconditions;

public class SuffStatUtils {
	public static SuffStats<?> sumStats(List<SuffStats<?>> suffStats) {

		Preconditions.checkArgument(suffStats.size() > 0, "Need more than zero data points.");
		SuffStats<?> summedStats = suffStats.get(0).create();

		for (SuffStats<?> dataPoint : suffStats) {
			summedStats.add(dataPoint);
		}
		return summedStats;
	}
}
