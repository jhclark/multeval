package multeval.metrics;

import jannopts.*;

import java.util.*;

import jbleu.*;

import com.google.common.base.*;
import com.google.common.collect.*;

// a MultiMetric wrapper around the jBLEU metric
public class Length extends Metric<IntStats> {

  @Override
  public String getMetricDescription() {
    return "Hypothesis length over reference length as a percent";
  }

  @Override
  public IntStats stats(String hyp, List<String> refs) {

    List<String> hypToks = Lists.newArrayList(Splitter.on(CharMatcher.BREAKING_WHITESPACE).split(hyp));
    List<List<String>> tokRefs = BLEU.tokenizeRefs(refs);
    int iRef = JBLEU.pickReference(hypToks, tokRefs);

    IntStats result = new IntStats(2);
    result.arr[0] = hypToks.size();
    result.arr[1] = tokRefs.get(iRef).size();
    return result;
  }

  @Override
  public double score(IntStats suffStats) {
    int hypLen = suffStats.arr[0];
    int refLen = suffStats.arr[1];
    return ((double) hypLen / (double) refLen) * 100;
  }

  @Override
  public String toString() {
    return "Length";
  }

  @Override
  public void configure(Configurator opts) throws ConfigurationException {
    opts.configure(this);
  }

  @Override
  public boolean isBiggerBetter() {
    return true;
  }
}
