package multeval.metrics;

import jannopts.*;

import java.util.*;

import multeval.util.*;
import ter.*;

import com.google.common.base.*;

public class TER extends Metric<IntStats> {

  @Option(shortName = "P", longName = "ter.punctuation", usage = "Ignore punctuation in TER?", defaultValue = "false")
  boolean punctuation;

  @Option(shortName = "b", longName = "ter.beamWidth", usage = "Beam width for TER", defaultValue = "20")
  int beamWidth;

  @Option(shortName = "d", longName = "ter.maxShiftDistance", usage = "Maximum shift distance for TER", defaultValue = "50")
  int maxShiftDistance;

  @Option(shortName = "M", longName = "ter.matchCost", usage = "Match cost for TER", defaultValue = "0.0")
  float matchCost;

  @Option(shortName = "D", longName = "ter.deleteCost", usage = "Delete cost for TER", defaultValue = "1.0")
  float deleteCost;

  @Option(shortName = "B", longName = "ter.substituteCost", usage = "Substitute cost for TER", defaultValue = "1.0")
  float substituteCost;

  @Option(shortName = "I", longName = "ter.insertCost", usage = "Insert cost for TER", defaultValue = "1.0")
  float insertCost;

  @Option(shortName = "T", longName = "ter.shiftCost", usage = "Shift cost for TER", defaultValue = "1.0")
  float shiftCost;

  private TERcost costfunc;
  private TERcalc calc = new TERcalc();
  private Configurator opts = null;

  @Override
  public String getMetricDescription() {
    return "Translation Error Rate (TER) V0.8.0";
  }

  @Override
  public IntStats stats(String hyp, List<String> refs) {

    double totwords = 0;
    TERalignment bestResult = null;

    // TER makes heinous use of static methods. we must synchronize to prevent disaster
    // when multi-threading at the hypothesis level (as we do in n-best scoring)
    // even so, it's better to just thread at the hypothesis level

    // number of words is average over references
    calc.setRefLen(refs);
    /* For each reference, compute the TER */
    for(int i = 0; i < refs.size(); ++i) {
	String ref = refs.get(i);
	
	TERalignment alignResult = calc.TER(hyp, ref, costfunc);
	
	if ((bestResult == null) || (bestResult.numEdits > alignResult.numEdits)) {
	    bestResult = alignResult;
	}
	
	totwords += alignResult.numWords;
    }

    bestResult.numWords = ((double) totwords) / ((double) refs.size());
    // if(!refids.isEmpty()) bestResult.bestRef = bestref;

    // now save the minimal sufficient statistics
    IntStats result = new IntStats(2);
    result.arr[0] = (int) bestResult.numEdits;
    result.arr[1] = (int) bestResult.numWords;
    return result;
  }

  @Override
  public double score(IntStats suffStats) {
    Preconditions.checkArgument(suffStats.arr.length == 2, "TER sufficient stats must be of length 2");
    int edits = suffStats.arr[0];
    int words = suffStats.arr[1];
    double score = ((double) edits / (double) words);
    return score * 100;
  }

  @Override
  public String toString() {
    return "TER";
  }

  @Override
  public void configure(Configurator opts) throws ConfigurationException {
    this.opts = opts;
    LibUtil.checkLibrary("ter.TERpara", "TER");
    opts.configure(this);
    configureLibTer();
  }
  
  private void configureLibTer() {
    costfunc = new TERcost();
    costfunc._delete_cost = deleteCost;
    costfunc._insert_cost = insertCost;
    costfunc._shift_cost = shiftCost;
    costfunc._match_cost = matchCost;
    costfunc._substitute_cost = substituteCost;

    // TERcalc.setNormalize(normalized);
    // TERcalc.setCase(caseon);
    calc.setPunct(punctuation);
    calc.setBeamWidth(beamWidth);
    calc.setShiftDist(maxShiftDistance);
  }

  @Override
  public boolean isBiggerBetter() {
    return false;
  }
  
  @Override
  public Metric<?> threadClone() {
      TER ter = new TER();
      try {
          opts.configure(ter);
    	  ter.configureLibTer();
      } catch(ConfigurationException e) {
	  // if this should happen, it should have already happened during the initial call to configure, never here
	  throw new RuntimeException(e);
      }
      return ter;
  }
}
