package multeval.significance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import multeval.metrics.Metric;
import multeval.util.ArrayUtils;
import multeval.util.MathUtils;
import multeval.util.SuffStatUtils;

import com.google.common.base.Preconditions;

public class BootstrapResampler {

	private final Random random = new Random();
	private final List<Metric> metrics;
	private final List<List<float[]>> suffStats;
	private int totalDataPoints;

	/**
	 * 
	 * @param suffStats
	 *            First list corresponds to the metrics, the
	 *            second dimension is number of data points (i.e. sentences)
	 *            and the inner array is the sufficient statistics for each metric.
	 */
	public BootstrapResampler(List<Metric> metrics,
			List<List<float[]>> suffStats) {
		
		Preconditions.checkArgument(metrics.size() > 0, "Must have at least one metric.");
		Preconditions.checkArgument(suffStats.size() > 0, "Must have at least one data point.");
		// TODO: Check for sufficient stats and metric count under each data point being parallel
		
		this.metrics = metrics;
		this.suffStats = suffStats;
		this.totalDataPoints = suffStats.get(0).size();
		
		Preconditions.checkArgument(totalDataPoints > 0, "Need more than zero data points.");
	}

	/**
	 * Returns a list whose size corresponds to the number of metrics being used
	 * for this resampler. Each inner double array contains a n metric values,
	 * calculated by applying the metric to the sufficient statistics aggregated
	 * from each resampling.
	 * 
	 * @param sampleSize
	 *            Size of each of the n re-sampled groups taken from the set of
	 *            points passed to the contructor.
	 * @param numSamples
	 *            Number of resampled groups to be drawn.
	 * @return
	 */
	public List<double[]> resample(int numSamples) {
		
		List<double[]> metricValues = new ArrayList<double[]>(metrics.size());
		for(int i=0; i<metrics.size(); i++) {
			metricValues.add(new double[numSamples]);
		}
		
		int sampleSize = totalDataPoints;
		int[] sampleMembers = new int[sampleSize];
		for (int iSample = 0; iSample < numSamples; iSample++) {
			chooseSampleMembers(totalDataPoints, sampleMembers);
			// NOTE: We could dump the sample members for analysis here if we wanted
			for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				float[] summedStats = sumStats(sampleMembers, iMetric, suffStats);
				Metric metric = metrics.get(iMetric);
				double score = metric.score(summedStats);
				metricValues.get(iMetric)[iSample] = score;
			}
		}
		return metricValues;
	}

	private static float[] sumStats(int[] sampleMembers,
			int iMetric, List<List<float[]>> suffStats) {
		
		int numMetricStats = suffStats.get(iMetric).get(0).length;
		float[] summedStats = new float[numMetricStats];

		for(int dataIdx : sampleMembers) {
			ArrayUtils.plusEquals(summedStats, suffStats.get(iMetric).get(dataIdx));
		}
		return summedStats;
	}

	private void chooseSampleMembers(int totalPointsAvailable, int[] sampleMembersOut) {
		for(int i = 0; i<sampleMembersOut.length; i++) {
			sampleMembersOut[i] = random.nextInt(totalPointsAvailable);
		}
	}
	
	private static void saveSamples(File scoresOut, double[] scores) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(scoresOut);
		for(double score : scores) {
			out.println(score);
		}
		out.close();
		System.err.println("Saved " + scores.length + " resampled metric scores to " + scoresOut.getAbsolutePath());
	}

	private static void printOverallInfo(String metricName, Metric metric, List<float[]> stats) {
		float[] summedStats = SuffStatUtils.sumStats(stats);
		double score = metric.score(summedStats);
		System.err.println("Overall " + metricName + " stats: score = " + score*100);
	}
	
	private static void printResampleStats(String metricName, double[] samples) {
		double mean = MathUtils.average(samples)*100;
		double stddev = Math.sqrt(MathUtils.variance(samples))*100;
		double min = MathUtils.min(samples)*100;
		double max = MathUtils.max(samples)*100;
		System.err.printf("Resampled %s stats: mean = %.2f stddev = %.2f min = %.2f max = %.2f\n", metricName, mean, stddev, min, max);
	}
	
//	public static void main(String[] args) throws IOException {
//		if(args.length != 7) {
//			System.err.println("Usage: program bleuSuffstatsIn meteorSuffstatsIn terSuffstatsIn bleuScoresOut meteorScoresOut terScoresOut numSamples");
//			System.exit(1);
//		}
//		File bleuStatsIn = new File(args[0]);
//		File meteorStatsIn = new File(args[1]);
//		File terStatsIn = new File(args[2]);
//		File bleuScoresOut = new File(args[3]);
//		File meteorScoresOut = new File(args[4]);
//		File terScoresOut = new File(args[5]);
//		int numSamples = Integer.parseInt(args[6]);
//		
//		List<Metric> metrics = Arrays.asList(new BLEU(), new METEOR(METEOR.RANKING_EN_WEIGHTS), new TER());
//		List<List<double[]>> suffStats = new ArrayList<List<double[]>>(metrics.size());
//		suffStats.add(SuffStatUtils.loadSuffStats(bleuStatsIn));
//		suffStats.add(SuffStatUtils.loadSuffStats(meteorStatsIn));
//		suffStats.add(SuffStatUtils.loadSuffStats(terStatsIn));
//		
//		printOverallInfo("BLEU", metrics.get(0), suffStats.get(0));
//		printOverallInfo("METEOR", metrics.get(1), suffStats.get(1));
//		printOverallInfo("TER", metrics.get(2), suffStats.get(2));
//		
//		System.err.println("Performing bootstrap resampling...");
//		BootstrapResampler resampler = new BootstrapResampler(metrics, suffStats);
//		List<double[]> samples = resampler.resample(numSamples);
//		
//		printResampleStats("BLEU", samples.get(0));
//		printResampleStats("METEOR", samples.get(1));
//		printResampleStats("TER", samples.get(2));
//		
//		saveSamples(bleuScoresOut, samples.get(0));
//		saveSamples(meteorScoresOut, samples.get(1));
//		saveSamples(terScoresOut, samples.get(2));
//	}
}
