package multeval.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.base.Preconditions;

public class SuffStatUtils {
	public static float[] sumStats(List<float[]> suffStats) {
		Preconditions.checkArgument(suffStats.size() > 0, "Need more than zero data points.");
		
		int numMetricStats = suffStats.get(0).length;
		float[] summedStats = new float[numMetricStats];

		for(float[] dataPoint : suffStats) {
			ArrayUtils.plusEquals(summedStats, dataPoint);
		}
		return summedStats;
	}

	public static List<float[]> loadSuffStats(File statsIn) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(statsIn));
		List<float[]> stats = new ArrayList<float[]>();
		String line;
		while((line = in.readLine()) != null) {
			StringTokenizer tok = new StringTokenizer(line);
			int numToks = tok.countTokens();
			float[] lineStats = new float[numToks];
			for(int i=0; i<numToks; i++) {
				lineStats[i] = Float.parseFloat(tok.nextToken());
			}
			stats.add(lineStats);
		}
		System.err.println("Loaded " + stats.size() + " data points from " + statsIn.getAbsolutePath());
		return stats;
	}
}
