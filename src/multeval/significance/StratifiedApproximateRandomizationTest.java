package multeval.significance;

import java.util.List;
import java.util.Random;

import multeval.metrics.Metric;
import multeval.util.ArrayUtils;
import multeval.util.SuffStatUtils;

import com.google.common.base.Preconditions;

public class StratifiedApproximateRandomizationTest {

	private static final Random random = new Random();
	private final List<Metric> metrics;

	private final List<List<float[]>> suffStatsA;
	private final List<List<float[]>> suffStatsB;
	private int totalDataPoints;

	/**
	 * @param suffStats
	 *            First list corresponds to the metrics, the second dimension is
	 *            number of data points (i.e. sentences) and the inner array is
	 *            the sufficient statistics for each metric.
	 */
	public StratifiedApproximateRandomizationTest(List<Metric> metrics,
			List<List<float[]>> suffStatsA, List<List<float[]>> suffStatsB) {

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
			Metric metric = metrics.get(iMetric);
			scoresA[iMetric] = metric.score(SuffStatUtils.sumStats(suffStatsA.get(iMetric)));
			scoresB[iMetric] = metric.score(SuffStatUtils.sumStats(suffStatsB.get(iMetric)));
			overallDiffs[iMetric] = Math.abs(scoresA[iMetric] - scoresB[iMetric]);
		}

		int[] diffsByChance = new int[metrics.size()];
		boolean[] shuffling = new boolean[totalDataPoints];
		for (int i = 0; i < numShuffles; i++) {
			chooseShuffling(shuffling);
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				Metric metric = metrics.get(iMetric);

				double scoreX = metric.score(sumStats(shuffling, iMetric, suffStatsA, suffStatsB));
				double scoreY = metric.score(sumStats(shuffling, iMetric, suffStatsB, suffStatsA));
				double sampleDiff = Math.abs(scoreX - scoreY);
				System.out.println(iMetric + ": " + scoreX + " - " + scoreY + " " + sampleDiff + " <> " + overallDiffs[iMetric]);
				if (sampleDiff >= overallDiffs[iMetric]) {
					diffsByChance[iMetric]++;
				}
			}
		}

		double[] p = new double[metrics.size()];
		for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			p[iMetric] = ((double) diffsByChance[iMetric] + 1.0) / ((double) numShuffles + 1.0);
		}
		return p;
	}

	private float[] sumStats(boolean[] shuffling,int iMetric,
			List<List<float[]>> suffStatsA, List<List<float[]>> suffStatsB) {

		int numStats = suffStatsA.get(iMetric).get(0).length;
		float[] summedStats = new float[numStats];
		List<float[]> metricStatsA = suffStatsA.get(iMetric);
		List<float[]> metricStatsB = suffStatsB.get(iMetric);
		for (int iRow =0; iRow<metricStatsA.size();iRow++) {
			float[] row = shuffling[iRow] ? metricStatsA.get(iRow) : metricStatsB.get(iRow);
			ArrayUtils.plusEquals(summedStats, row);
		}
		return summedStats;
	}

	private static void chooseShuffling(boolean[] shuffling) {
		for (int i = 0; i < shuffling.length; i++) {
			shuffling[i] = random.nextBoolean();
		}
	}
	
//	public static void main(String[] args) throws IOException {
//		if(args.length != 7) { 
//			System.err.println("Usage: program bleuStatsA meteorStatsA terStatsA bleuStatsB meteorStatsB terStatsB numShuffles");
//			System.exit(1);
//		}
//		
//		File bleuStatsA = new File(args[0]);
//		File meteorStatsA = new File(args[1]);
//		File terStatsA = new File(args[2]);
//		File bleuStatsB = new File(args[3]);
//		File meteorStatsB = new File(args[4]);
//		File terStatsB = new File(args[5]);
//		int numShuffles = Integer.parseInt(args[6]);
//		
//		List<Metric> metrics = Arrays.asList(new BLEU(), new METEOR(METEOR.RANKING_EN_WEIGHTS), new TER());
//		List<List<double[]>> suffStatsA = new ArrayList<List<double[]>>(metrics.size());
//		suffStatsA.add(SuffStatUtils.loadSuffStats(bleuStatsA));
//		suffStatsA.add(SuffStatUtils.loadSuffStats(meteorStatsA));
//		suffStatsA.add(SuffStatUtils.loadSuffStats(terStatsA));
//		
//		List<List<double[]>> suffStatsB = new ArrayList<List<double[]>>(metrics.size());
//		suffStatsB.add(SuffStatUtils.loadSuffStats(bleuStatsB));
//		suffStatsB.add(SuffStatUtils.loadSuffStats(meteorStatsB));
//		suffStatsB.add(SuffStatUtils.loadSuffStats(terStatsB));
//		
//		StratifiedApproximateRandomizationTest test = new StratifiedApproximateRandomizationTest(metrics, suffStatsA, suffStatsB);
//		System.out.println("Running test with " + numShuffles + " shuffles");
//		double[] p = test.getTwoSidedP(numShuffles);
//		System.out.println("BLEU p-value: " + p[0]);
//		System.out.println("METEOR p-value: " + p[1]);
//		System.out.println("TER p-value: " + p[2]);
//	}
}
