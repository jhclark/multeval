package multeval;

import jannopts.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * System index 0 is the baseline system
 * 
 * @author jon
 *
 */
public class HypothesisManager {

	private ArrayList<List<String>> allRefs;
	private int numHyps = -1;
	private int numRefs = -1;
	private int numOptRuns = -1;
	private int numSystems = -1;
	
	public int getNumHyps() {
		return numHyps;
	}
	
	public int getNumOptRuns() {
		return numOptRuns;
	}
	
	public int getNumRefs() {
		return numRefs;
	}

	public void loadData(String[] hypFilesBaseline, String[][] hypFilesBySys, String[] refFiles) {

		numSystems = hypFilesBySys.length;
		for (String[] hypFiles : hypFilesBySys) {
			if (numOptRuns == -1) {
				numOptRuns = hypFiles.length;
			} else {
				if (numOptRuns != hypFiles.length) {
					throw new RuntimeException(
							"Non-parallel number of optimization runs. Expected " + numOptRuns
									+ " but got " + hypFiles.length);
				}
			}

			for (String hypFile : hypFiles) {
				List<String> hypsForOptRun = loadSentences(hypFile);
				allHyps.add(hypsForOptRun);

				if (numHyps == -1) {
					numHyps = hypsForOptRun.size();
				} else {
					if (numHyps != hypsForOptRun.size()) {
						throw new RuntimeException("Non-parallel inputs detected. Expected "
								+ numHyps + " hypotheses, but got " + hypsForOptRun.size());
					}
				}
			}
		}
		
		// TODO: Auto-detect laced refs?
		
		int numSystems = hypFilesBySys.length;

		// first index is sentence number, second is reference number
		allRefs = new ArrayList<List<String>>();
		numRefs = refFiles.length;
		for (String refFile : refFiles) {
			List<String> oneRefPerHyp = loadSentences(refFile);
			if (numHyps != oneRefPerHyp.size()) {
				throw new RuntimeException("Non-parallel inputs detected. Expected " + numHyps
						+ " references, but got " + oneRefPerHyp.size());
			}

			for (int i = 0; i < oneRefPerHyp.size(); i++) {
				String ref = oneRefPerHyp.get(i);
				while (allRefs.size() <= i) {
					allRefs.add(new ArrayList<String>(numRefs));
				}
				allRefs.get(i).add(ref);
			}
		}
	}

	public static List<String> loadSentences(String hypFile) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getNumSystems() {
		return numSystems;
	}

	public String getHypothesis(int iSys, int iOpt, int iHyp) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getReferences(int iSys, int iOpt, int iHyp) {
		// TODO Auto-generated method stub
		return null;
	}
}
