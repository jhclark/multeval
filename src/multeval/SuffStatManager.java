package multeval;

import java.util.ArrayList;
import java.util.List;

public class SuffStatManager {

	// indexices iSys, iOpt, iMetric, iHyp; inner array: various suff stats for
	// a particular metric
	private final List<List<List<List<float[]>>>> statsBySys;
	private final int numMetrics;
	private final int numOpt;
	private final int numHyp;

	private static final float[] DUMMY = new float[0];

	public SuffStatManager(int numMetrics, int numSys, int numOpt, int numHyp) {
		this.numMetrics = numMetrics;
		this.numOpt = numOpt;
		this.numHyp = numHyp;
		this.statsBySys = new ArrayList<List<List<List<float[]>>>>(numSys);
	}

	public void saveStats(int iMetric, int iSys, int iOpt, int iHyp, float[] stats) {
		// first, expand as necessary
		// TODO: Use more intelligent list type that allows batch grow
		// operations
		while (statsBySys.size() <= iSys) {
			statsBySys.add(new ArrayList<List<List<float[]>>>(numOpt));
		}
		List<List<List<float[]>>> statsByOpt = statsBySys.get(iSys);
		while (statsByOpt.size() <= iOpt) {
			statsByOpt.add(new ArrayList<List<float[]>>(numMetrics));
		}
		List<List<float[]>> statsByMetric = statsByOpt.get(iOpt);
		while (statsByMetric.size() <= iMetric) {
			statsByMetric.add(new ArrayList<float[]>(numHyp));
		}
		List<float[]> statsByHyp = statsByMetric.get(iMetric);
		while (statsByHyp.size() <= iHyp) {
			statsByHyp.add(DUMMY);
		}
		statsByHyp.set(iHyp, stats);
	}
	
	// inner array: various suff stats for a particular metric
	public float[] getStats(int iMetric, int iSys, int iOpt, int iHyp) {
		// TODO: More informative error messages w/ bounds checking
		return getStats(iMetric, iSys, iOpt).get(iHyp);
	}

	// list index: iHyp; inner array: various suff stats for a particular metric
	public List<float[]> getStats(int iMetric, int iSys, int iOpt) {
		// TODO: More informative error messages w/ bounds checking
		return getStats(iSys, iOpt).get(iMetric);
	}

	// indices: iMetric, iHyp
	public List<List<float[]>> getStats(int iSys, int iOpt) {
		// TODO: More informative error messages w/ bounds checking
		return statsBySys.get(iSys).get(iOpt);
	}

	// appends all optimization runs together
	// indices: iMetric, iHyp; inner array: various suff stats for a particular
	// metric
	public List<List<float[]>> getStatsAllOptForSys(int iSys) {

		List<List<float[]>> resultByMetric = new ArrayList<List<float[]>>(numMetrics);
		
		List<List<List<float[]>>> statsByOpt = statsBySys.get(iSys);
		
		for(int iMetric=0; iMetric<numMetrics; iMetric++) {
			ArrayList<float[]> resultByHyp = new ArrayList<float[]>(numHyp * numOpt);
			resultByMetric.add(resultByHyp);
			for (int iOpt = 0; iOpt < numOpt; iOpt++) {
				List<List<float[]>> statsByMetric = statsByOpt.get(iOpt);
				List<float[]> statsByHyp = statsByMetric.get(iMetric);
				resultByHyp.addAll(statsByHyp); 
			}
		}
		return resultByMetric;
	}
}
