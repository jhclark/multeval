package multeval.significance;

import java.util.List;
import java.util.Random;

import multeval.metrics.Metric;
import multeval.metrics.SuffStats;
import multeval.util.SuffStatUtils;

import com.google.common.base.Preconditions;

public class StratifiedApproximateRandomizationTest {

	private static final Random random = new Random();
	private final List<Metric<?>> metrics;

	private final List<List<SuffStats<?>>> suffStatsA;
	private final List<List<SuffStats<?>>> suffStatsB;
	private int totalDataPoints;

	/**
	 * @param suffStats
	 *            First list corresponds to the metrics, the second dimension is
	 *            number of data points (i.e. sentences) and the inner array is
	 *            the sufficient statistics for each metric.
	 */
	public StratifiedApproximateRandomizationTest(List<Metric<?>> metrics,
			List<List<SuffStats<?>>> suffStatsA, List<List<SuffStats<?>>> suffStatsB) {

		Preconditions.checkArgument(metrics.size() > 0, "Must have at least one metric.");
		Preconditions.checkArgument(suffStatsA.size() > 0, "Must have at least one data point.");
		// TODO: Check for sufficient stats and metric count under each data
		// point being parallel (same for BootstrapResampler)

		this.metrics = metrics;
		this.suffStatsA = suffStatsA;
		this.suffStatsB = suffStatsB;
		this.totalDataPoints = suffStatsA.get(0).size();
		Preconditions.checkArgument(suffStatsA.get(0).size() == suffStatsB.get(0).size(),
				"System A and System B must have the same number of data points.");

		Preconditions.checkArgument(totalDataPoints > 0, "Need more than zero data points.");
	}

	public double[] getTwoSidedP(int numShuffles) {

		double[] overallDiffs = new double[metrics.size()];
		double[] scoresA = new double[metrics.size()];
		double[] scoresB = new double[metrics.size()];

		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			Metric<?> metric = metrics.get(iMetric);
			scoresA[iMetric] = metric.scoreStats(SuffStatUtils.sumStats(suffStatsA.get(iMetric)));
			scoresB[iMetric] = metric.scoreStats(SuffStatUtils.sumStats(suffStatsB.get(iMetric)));
			overallDiffs[iMetric] = Math.abs(scoresA[iMetric] - scoresB[iMetric]);
		}

		int[] diffsByChance = new int[metrics.size()];
		boolean[] shuffling = new boolean[totalDataPoints];
		for (int i = 0; i < numShuffles; i++) {
			chooseShuffling(shuffling);
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				Metric<?> metric = metrics.get(iMetric);

				double scoreX = metric.scoreStats(sumStats(shuffling, iMetric, suffStatsA, suffStatsB));
				double scoreY = metric.scoreStats(sumStats(shuffling, iMetric, suffStatsB, suffStatsA));
				double sampleDiff = Math.abs(scoreX - scoreY);
//				System.out.println(iMetric + ": " + scoreX + " - " + scoreY + " " + sampleDiff + " <> " + overallDiffs[iMetric]);
				// the != is important. if we want to score the same system against itself,
				// having a zero difference should not be attributed to chance.
				if (sampleDiff > overallDiffs[iMetric]) {
					diffsByChance[iMetric]++;
				}
			}
		}

		double[] p = new double[metrics.size()];
		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			// +1 applies here, though it only matters for small numbers of
			// shufflings, which we typically never do. it's necessary to ensure
			// the probability of falsely rejecting the null hypothesis is no
			// greater than the rejection level of the test (see william
			// morgan on significance tests)
			p[iMetric] = ((double) diffsByChance[iMetric] + 1.0) / ((double) numShuffles + 1.0);
		}
		return p;
	}

	private static SuffStats<?> sumStats(boolean[] shuffling, int iMetric,
			List<List<SuffStats<?>>> suffStatsA, List<List<SuffStats<?>>> suffStatsB) {

		SuffStats<?> summedStats = suffStatsA.get(iMetric).get(0).create();
		List<SuffStats<?>> metricStatsA = suffStatsA.get(iMetric);
		List<SuffStats<?>> metricStatsB = suffStatsB.get(iMetric);
		for (int iRow = 0; iRow < metricStatsA.size(); iRow++) {
			SuffStats<?> row = shuffling[iRow] ? metricStatsA.get(iRow) : metricStatsB.get(iRow);
			summedStats.add(row);
		}
		return summedStats;
	}

	private static void chooseShuffling(boolean[] shuffling) {
		for (int i = 0; i < shuffling.length; i++) {
			shuffling[i] = random.nextBoolean();
		}
	}
}
