package multeval.metrics;

import jannopts.*;

import java.util.*;

import jbleu.*;
import multeval.util.*;

import com.google.common.base.*;
import com.google.common.collect.*;

// a MultiMetric wrapper around the jBLEU metric
public class BLEU extends Metric<IntStats> {

  // @Option(shortName = "c", longName = "bleu.closestRefLength", usage =
  // "Use closest reference length when determining brevity penalty? (true behaves like IBM BLEU, false behaves like old NIST BLEU)",
  // defaultValue="true")
  // boolean closestRefLength;

  private JBLEU bleu = new JBLEU();

  public static final String[] SUBMETRIC_NAMES = { "bleu1p", "bleu2p", "bleu3p", "bleu4p", "brevity" };

  @Override
  public IntStats stats(String hyp, List<String> refs) {

    List<String> tokHyp = Lists.newArrayList(Splitter.on(CharMatcher.BREAKING_WHITESPACE).split(hyp));
    List<List<String>> tokRefs = tokenizeRefs(refs);

    IntStats result = new IntStats(JBLEU.getSuffStatCount());
    bleu.stats(tokHyp, tokRefs, result.arr);
    return result;
  }

  public static List<List<String>> tokenizeRefs(List<String> refs) {
    List<List<String>> tokRefs = new ArrayList<List<String>>();
    for(String ref : refs) {
      tokRefs.add(Lists.newArrayList(Splitter.on(CharMatcher.BREAKING_WHITESPACE).split(ref)));
    }
    return tokRefs;
  }

  @Override
  public String getMetricDescription() {
    return String.format("jBLEU V%s (an exact reimplementation of NIST's mteval-v13.pl without tokenization)".format(JBLEU.VERSION));
  }

  @Override
  public double score(IntStats suffStats) {
    return bleu.score(suffStats.arr) * 100;
  }

  @Override
  public double[] scoreSubmetrics(IntStats suffStats) {
    int N = 4;
    double[] result = new double[N + 1];
    bleu.score(suffStats.arr, result);
    return result;
  }

  @Override
  public String[] getSubmetricNames() {
    return SUBMETRIC_NAMES;
  }

  @Override
  public String toString() {
    return "BLEU";
  }

  @Override
  public void configure(Configurator opts) throws ConfigurationException {
    LibUtil.checkLibrary("jbleu.JBLEU", "jBLEU");
    opts.configure(this);
  }

  @Override
  public boolean isBiggerBetter() {
    return true;
  }
}
