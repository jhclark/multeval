package multeval.metrics;

import jannopts.Option;

import java.util.List;

import ter.TERalignment;
import ter.TERcalc;
import ter.TERcost;

import com.google.common.base.Preconditions;

public class TER implements Metric {
	
	@Option(shortName = "P", longName = "ter.punctuation", usage = "Use punctuation in TER?", defaultValue = "false")
	boolean punctuation;
	
	@Option(shortName = "b", longName = "ter.beamWidth", usage = "Beam width for TER", defaultValue="20")
	int beamWidth;
	
	@Option(shortName = "d", longName = "ter.maxShiftDistance", usage = "Maximum shift distance for TER", defaultValue="50")
	int maxShiftDistance;
	
	@Option(shortName = "M", longName = "ter.matchCost", usage = "Match cost for TER", defaultValue="0.0")
	float matchCost;
	
	@Option(shortName = "D", longName = "ter.deleteCost", usage = "Delete cost for TER", defaultValue="1.0")
	float deleteCost;
	
	@Option(shortName = "B", longName = "ter.substituteCost", usage = "Substitute cost for TER", defaultValue="1.0")
	float substituteCost;
	
	@Option(shortName = "I", longName = "ter.insertCost", usage = "Insert cost for TER", defaultValue="1.0")
	float insertCost;
	
	@Option(shortName = "T", longName = "ter.shiftCost", usage = "Shift cost for TER", defaultValue="1.0")
	float shiftCost;

	private TERcost costfunc;
	
	public TER() {
        costfunc = new TERcost();
        costfunc._delete_cost = deleteCost;
        costfunc._insert_cost = insertCost;
        costfunc._shift_cost = shiftCost;
        costfunc._match_cost = matchCost;
        costfunc._substitute_cost = substituteCost;
        
//        TERcalc.setNormalize(normalized);
//        TERcalc.setCase(caseon);
        TERcalc.setPunct(punctuation);
        TERcalc.setBeamWidth(beamWidth);
        TERcalc.setShiftDist(maxShiftDistance);
	}

	@Override
	public float[] stats(String hyp, List<String> refs) {
		
		double totwords = 0;
        TERalignment bestResult = null;
        
        // number of words is average over references
        TERcalc.setRefLen(refs);
        /* For each reference, compute the TER */
        for (int i = 0; i < refs.size(); ++i) {
            String ref = refs.get(i);

            TERalignment alignResult = TERcalc.TER(hyp, ref, costfunc);

            if ((bestResult == null) || (bestResult.numEdits > alignResult.numEdits)) {
                bestResult = alignResult;
            }

            totwords += alignResult.numWords;
        }
        
        bestResult.numWords = ((double) totwords) / ((double) refs.size());
//        if(!refids.isEmpty()) bestResult.bestRef = bestref;
        
        // now save the minimal sufficient statistics
        float[] result = new float[2];
        result[0] = (float) bestResult.numEdits;
        result[1] = (float) bestResult.numWords;
        return result;
	}

	@Override
	public double score(float[] suffStats) {
		Preconditions.checkArgument(suffStats.length == 2, "TER sufficient stats must be of length 2");
		float edits = suffStats[0];
		float words = suffStats[1];
		return edits / words;
	}
	
	@Override
	public String toString() {
		return "TER";
	}
}
