package multeval;

import java.util.ArrayList;
import java.util.List;

public class SuffStatManager {
	// index 1: system, index 2: optimization run, index 3: metric, index 4: sentence
	List<List<List<float[]>>> suffStatsByMetric = new ArrayList<List<List<float[]>>>(metrics.size());

	public void saveStats(int iMetric, int iSys, int iOpt, int iHyp, float[] stats) {
		// TODO Auto-generated method stub
		
	}

	public List<float[]> getStats(int iMetric, int iSys, int iOpt) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<List<float[]>> getStats(int iSys, int iOpt) {
		// TODO Auto-generated method stub
		return null;
	}

	// index 1: metric, index 2: hypothesis, inner array: suff stats
	public List<List<double[]>> getStatsAllOptForSys(int i) {

		// TODO: We'll need all opt runs appended here

		// TODO Auto-generated method stub
		return null;
	}
}
