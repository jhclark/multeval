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
  private final int numHyps;
  private final int numOptRuns;
  
  private final boolean debug;

  /** @param suffStats First list corresponds to the metrics, the second
   *          dimension is number of data points (i.e. sentences) and the inner
   *          data structure is the sufficient statistics for each metric. 
   *          The number of data points must equal numHyps times numOptRuns */
  public StratifiedApproximateRandomizationTest(List<Metric<?>> metrics, List<List<SuffStats<?>>> suffStatsA,
      List<List<SuffStats<?>>> suffStatsB, int numHyps, int numOptRuns, boolean debug) {

    Preconditions.checkArgument(metrics.size() > 0, "Must have at least one metric.");
    Preconditions.checkArgument(suffStatsA.size() > 0, "Must have at least one data point.");
    // TODO: Check for sufficient stats and metric count under each data
    // point being parallel (same for BootstrapResampler)

    this.metrics = metrics;
    this.suffStatsA = suffStatsA;
    this.suffStatsB = suffStatsB;
    this.totalDataPoints = suffStatsA.get(0).size();
    this.numHyps = numHyps;
    this.numOptRuns = numOptRuns;
    
    this.debug = debug;
    
    Preconditions.checkArgument(suffStatsA.get(0).size() == suffStatsB.get(0).size(),
        "System A and System B must have the same number of data points.");

    Preconditions.checkArgument(totalDataPoints > 0, "Need more than zero data points.");
    Preconditions.checkArgument(totalDataPoints == numHyps * numOptRuns,
    		String.format("totalDataPoints (%d) in second list must == numHyps (%d) * numOptRuns (%d)", totalDataPoints, numHyps, numOptRuns));
  }

  public double[] getTwoSidedP(int numShuffles) {

    double[] overallDiffs = new double[metrics.size()];
    double[] scoresA = new double[metrics.size()];
    double[] scoresB = new double[metrics.size()];

    for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
      Metric<?> metric = metrics.get(iMetric);
      scoresA[iMetric] = metric.scoreStats(SuffStatUtils.sumStats(suffStatsA.get(iMetric)));
      scoresB[iMetric] = metric.scoreStats(SuffStatUtils.sumStats(suffStatsB.get(iMetric)));
      overallDiffs[iMetric] = Math.abs(scoresA[iMetric] - scoresB[iMetric]);
    }

    int[] diffsByChance = new int[metrics.size()];
    Shuffling shuffling = new Shuffling(numHyps, numOptRuns);
    for(int i = 0; i < numShuffles; i++) {
      shuffling.shuffle();
      for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
        Metric<?> metric = metrics.get(iMetric);

        double scoreX = metric.scoreStats(sumStats(shuffling, iMetric, suffStatsA, suffStatsB, false));
        double scoreY = metric.scoreStats(sumStats(shuffling, iMetric, suffStatsA, suffStatsB, true));
        double sampleDiff = Math.abs(scoreX - scoreY);
        // the != is important. if we want to score the same system against
        // itself,
        // having a zero difference should not be attributed to chance.
        if (sampleDiff > overallDiffs[iMetric]) {
          diffsByChance[iMetric]++;
        }
        if(debug) {
            System.err.println("DIFF metric " + iMetric + ": " + scoreX + " - " + scoreY + " --> " +
              sampleDiff + " >? " + overallDiffs[iMetric] + "; diffsByChance: " + diffsByChance[iMetric]);
          }
      }
    }

    double[] p = new double[metrics.size()];
    for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
      // +1 applies here, though it only matters for small numbers of
      // shufflings, which we typically never do. it's necessary to ensure
      // the probability of falsely rejecting the null hypothesis is no
      // greater than the rejection level of the test (see william
      // morgan on significance tests)
      p[iMetric] = ((double) diffsByChance[iMetric] + 1.0) / ((double) numShuffles + 1.0);
    }
    return p;
  }

  private static SuffStats<?> sumStats(Shuffling shuffling, int iMetric, List<List<SuffStats<?>>> suffStatsA,
      List<List<SuffStats<?>>> suffStatsB, boolean invert) {

    SuffStats<?> summedStats = suffStatsA.get(iMetric).get(0).create();
    List<SuffStats<?>> metricStatsA = suffStatsA.get(iMetric);
    List<SuffStats<?>> metricStatsB = suffStatsB.get(iMetric);
    for(int iRow = 0; iRow < metricStatsA.size(); iRow++) {
      SuffStats<?> row = shuffling.at(iRow, metricStatsA, metricStatsB, invert);
      summedStats.add(row);
    }
    return summedStats;
  }
  
  static class Shuffling {
	private final boolean[] swap;
	private final int[] optRunPermutation;
	private final int[] optRunPermutationInv;
	private final int optRuns;
	private final int hyps;
    private static final Random rnd = new Random();
	
	public Shuffling(int hyps, int optRuns) {
		this.swap = new boolean[hyps*optRuns];
		this.optRunPermutation = new int[hyps*optRuns];
		this.optRunPermutationInv = new int[hyps*optRuns];
		this.hyps = hyps;
		this.optRuns = optRuns;
	}
	
	public <T> T at(int iRow, List<T> a, List<T> b, boolean invert) {
		final boolean shouldSwap;
		final int idx;
		if(invert) {
			idx = optRunPermutationInv[iRow];
			shouldSwap = !swap[iRow];
		} else {
			idx = optRunPermutation[iRow];
			shouldSwap = swap[iRow];
		}
		
		final List<T> list = shouldSwap ? b : a;
		return list.get(idx);
	}

	// shuffle, stratifying on like hypotheses, but allowing swaps between systems and optimizer runs
	public void shuffle() {
		
		// decide swaps
	    for(int i = 0; i < swap.length; i++) {
	      swap[i] = random.nextBoolean();
	    }
	    
	    // decide how to permute sentences between optimization runs
	    
	    // 1) init by storing indices into original array
	    for(int i=0; i<optRunPermutation.length; i++) {
	    	optRunPermutation[i] = i;
	    }
	    
	    // 2) randomly permute like hypotheses between optimization runs
	    // based on Collections.shuffle, but for a primitive array...
	    // all permutations are equally likely given a fair source of randomness
	    for(int iHyp=0; iHyp<hyps; iHyp++) {
	    	for (int iRun=optRuns; iRun>1; iRun--) {
	    		int swapRun1 = iRun-1;
	    		int swapRun2 = rnd.nextInt(iRun);
	    		swap(optRunPermutation, iHyp + hyps*swapRun1, iHyp+hyps*swapRun2);
	    	}
	    }
	    
	    // now save inverse function
	    for(int origIdx=0; origIdx<optRunPermutation.length; origIdx++) {
	    	int mappedIdx = optRunPermutation[origIdx];
	    	optRunPermutationInv[mappedIdx] = origIdx;
	    }
	 }

	private void swap(int[] arr, int i, int j) {
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}
  }
}
