package multeval.significance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import multeval.metrics.Metric;
import multeval.metrics.SuffStats;

import com.google.common.base.Preconditions;

public class BootstrapResampler {

	private final Random random = new Random();
	private final List<Metric<?>> metrics;
	private final List<List<SuffStats<?>>> suffStats;
	private int totalDataPoints;

	/**
	 * 
	 * @param suffStats
	 *            First list corresponds to the metrics, the
	 *            second dimension is number of data points (i.e. sentences)
	 *            and the inner array is the sufficient statistics for each metric.
	 */
	public BootstrapResampler(List<Metric<?>> metrics,
			List<List<SuffStats<?>>> suffStats) {
		
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
				SuffStats<?> summedStats = sumStats(sampleMembers, iMetric, suffStats);
				
				// hack around generics by erasure
				Metric<?> metric = metrics.get(iMetric);
				double score = metric.scoreStats(summedStats);
				metricValues.get(iMetric)[iSample] = score;
			}
		}
		return metricValues;
	}

	private static SuffStats<?> sumStats(int[] sampleMembers,
			int iMetric, List<List<SuffStats<?>>> ss) {
		
		SuffStats<?> summedStats = ss.get(iMetric).get(0).create();

		for(int dataIdx : sampleMembers) {
			summedStats.add(ss.get(iMetric).get(dataIdx));
		}
		return summedStats;
	}

	private void chooseSampleMembers(int totalPointsAvailable, int[] sampleMembersOut) {
		for(int i = 0; i<sampleMembersOut.length; i++) {
			sampleMembersOut[i] = random.nextInt(totalPointsAvailable);
		}
	}

}
