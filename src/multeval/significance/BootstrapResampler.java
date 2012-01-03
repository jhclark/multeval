package multeval.significance;

import java.util.*;

import multeval.metrics.*;
import multeval.parallel.MetricWorkerPool;

import com.google.common.base.*;

public class BootstrapResampler {

  private final Random random = new Random();
  
  private final int threads;
  private final List<Metric<?>> masterMetrics;
  private final List<List<SuffStats<?>>> suffStats;
  private int totalDataPoints;

  /** @param suffStats First list corresponds to the metrics, the second
   *          dimension is number of data points (i.e. sentences) and the inner
   *          array is the sufficient statistics for each metric. */
  public BootstrapResampler(int threads, List<Metric<?>> metrics, List<List<SuffStats<?>>> suffStats) {

    Preconditions.checkArgument(metrics.size() > 0, "Must have at least one metric.");
    Preconditions.checkArgument(suffStats.size() > 0, "Must have at least one data point.");
    // TODO: Check for sufficient stats and metric count under each data point
    // being parallel

    this.threads = threads;
    this.masterMetrics = metrics;
    this.suffStats = suffStats;
    this.totalDataPoints = suffStats.get(0).size();

    Preconditions.checkArgument(totalDataPoints > 0, "Need more than zero data points.");
  }
  
  private static class Locals {
	    public final int[] sampleMembers;
	    public final List<Metric<?>> metrics;
	    
	    public Locals(List<Metric<?>> masterMetrics, int sampleSize) {
	    	 this.sampleMembers = new int[sampleSize];
	    	 this.metrics = new ArrayList<Metric<?>>(masterMetrics.size());
	    	 for(Metric<?> metric : masterMetrics) {
	    		 metrics.add(metric.threadClone());
	    	 }
	    }
  }

  /** Returns a list whose size corresponds to the number of metrics being used
   * for this resampler. Each inner double array contains a n metric values,
   * calculated by applying the metric to the sufficient statistics aggregated
   * from each resampling.
   * 
   * @param sampleSize Size of each of the n re-sampled groups taken from the
   *          set of points passed to the contructor.
   * @param numSamples Number of resampled groups to be drawn.
   * @return 
 * @throws InterruptedException */
  public List<double[]> resample(int numSamples) throws InterruptedException {

    final List<double[]> metricValues = new ArrayList<double[]>(masterMetrics.size());
    for(int i = 0; i < masterMetrics.size(); i++) {
      metricValues.add(new double[numSamples]);
    }
 
    MetricWorkerPool<Integer, Locals> workers = new MetricWorkerPool<Integer, Locals>(threads, new Supplier<Locals>() {
		@Override
		public Locals get() {
			return new Locals(masterMetrics, totalDataPoints);
		}
    }) {
		@Override
		public void doWork(Locals locals, Integer iSample) {
	      chooseSampleMembers(totalDataPoints, locals.sampleMembers);
	      // NOTE: We could dump the sample members for analysis here if we wanted
	      for(int iMetric = 0; iMetric < masterMetrics.size(); iMetric++) {  
	        SuffStats<?> summedStats = sumStats(locals.sampleMembers, iMetric, suffStats);

	        Metric<?> metric = locals.metrics.get(iMetric);
	        double score = metric.scoreStats(summedStats);
	        metricValues.get(iMetric)[iSample] = score;
	      }
		}
	};
    
	workers.start();
    for(int iSample = 0; iSample < numSamples; iSample++) {
    	workers.addTask(iSample);
    }
    workers.waitForCompletion();
    
    return metricValues;
  }

  private static SuffStats<?> sumStats(int[] sampleMembers, int iMetric, List<List<SuffStats<?>>> ss) {

    SuffStats<?> summedStats = ss.get(iMetric).get(0).create();

    for(int dataIdx : sampleMembers) {
      summedStats.add(ss.get(iMetric).get(dataIdx));
    }
    return summedStats;
  }

  private void chooseSampleMembers(int totalPointsAvailable, int[] sampleMembersOut) {
    for(int i = 0; i < sampleMembersOut.length; i++) {
      sampleMembersOut[i] = random.nextInt(totalPointsAvailable);
    }
  }

}
