package multeval.metrics;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;

import java.util.ArrayList;
import java.util.List;

import jbleu.JBLEU;
import multeval.util.LibUtil;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

// a MultiMetric wrapper around the jBLEU metric
public class BLEU extends Metric<IntStats> {

  // @Option(shortName = "c", longName = "bleu.closestRefLength", usage =
  // "Use closest reference length when determining brevity penalty? (true behaves like IBM BLEU, false behaves like old NIST BLEU)",
  // defaultValue="true")
  // boolean closestRefLength;

  @Option(shortName = "v", longName = "bleu.verbosity", usage = "Verbosity level (Integer: 0-1)", defaultValue = "0")
  public int verbosity;

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
    return String.format("jBLEU V%s (an exact reimplementation of NIST's mteval-v13.pl without tokenization)", JBLEU.VERSION);
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
    this.bleu.verbosity = this.verbosity;
  }

  @Override
  public boolean isBiggerBetter() {
    return true;
  }
}
