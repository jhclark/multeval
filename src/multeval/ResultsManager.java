package multeval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsManager {

	// indices: iSys, iMetric
	private final List<List<Map<Type, Double>>> resultsBySys;
	private int numMetrics;
	public final String[] metricNames;
	public final String[] sysNames;
	
	public enum Type {AVG, STDDEV, MIN, MAX, RESAMPLED_MEAN_AVG, RESAMPLED_STDDEV_AVG, RESAMPLED_MIN, RESAMPLED_MAX, P_VALUE}

	public ResultsManager(String[] metricNames, String[] sysNames) {
		this.metricNames = metricNames;
		this.sysNames = sysNames;
		
		this.numMetrics = metricNames.length;
		int numSys = sysNames.length;
		this.resultsBySys = new ArrayList<List<Map<Type, Double>>>(numSys);
	}

	public void report(int iMetric, int iSys, Type type, double d) {
		while(resultsBySys.size() <= iSys) {
			resultsBySys.add(new ArrayList<Map<Type, Double>>(numMetrics));
		}
		List<Map<Type, Double>> resultsByMetric = resultsBySys.get(iSys);
		while(resultsByMetric.size() <= iMetric) {
			resultsByMetric.add(new HashMap<Type, Double>());
		}
		Map<Type, Double> map = resultsByMetric.get(iMetric);
		map.put(type, d);
		
		System.err.println("GOT RESULT: " + sysNames[iSys] + ": " + metricNames[iMetric] + ": " + type.name() + ": " + String.format("%.2f", d*100));
	}
	
	public Double get(int iMetric, int iSys, Type type) {
		List<Map<Type, Double>> resultsByMetric = resultsBySys.get(iSys);
		if(resultsByMetric == null) {
			throw new RuntimeException("No results found for system + " + iSys);
		}
		Map<Type, Double> resultsByType = resultsByMetric.get(iMetric);
		if(resultsByType == null) {
			throw new RuntimeException("No results found for system " + iSys + " for metric " + iMetric);
		}
		Double result = resultsByType.get(type);
		if(result == null) {
			throw new RuntimeException("No results found for system " + iSys + " for metric " + iMetric + " of type " + type);			
		}
		return result;
	}
}
