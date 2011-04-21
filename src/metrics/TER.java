package metrics;

import jannopts.Option;

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
	
	public TER() {
        TERcost costfunc = new TERcost();
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
	public float stats(String[] sentence, String[][] refs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double score(double[] suffStats) {
		// TODO Auto-generated method stub
		return 0;
	}
}
