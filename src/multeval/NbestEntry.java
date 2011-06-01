package multeval;

import java.util.*;

import multeval.metrics.*;

import com.google.common.base.*;

public class NbestEntry {
  public int sentId;
  public int origRank;
  public String hyp;
  public String feats;
  public float total;

  public double[] metricScores;
  public int[] metricRank;

  public double[] submetricScores;
  public List<SuffStats<?>> metricStats;

  public static NbestEntry parse(String cdecStr, int origRank, int numMetrics) {

    NbestEntry result = new NbestEntry();

    // 0 ||| the transactions with the shares of čez achieved almost half of
    // the normal agenda . ||| Glue=8 LanguageModel=-39.2525 PassThrough=1
    // PhraseModel_0=2.18572 PhraseModel_1=13.4858 PhraseModel_2=4.24232
    // WordPenalty=-6.51442 ContextCRF=-35.9812 crf.ContentWordCount=7
    // crf.NonContentWordCount=26 crf.StopWordCount=7
    // crf.NonStopWordCount=26 ||| -28.842
    Iterator<String> columns = Splitter.on(" ||| ").split(cdecStr).iterator();
    result.sentId = Integer.parseInt(columns.next());
    result.hyp = columns.next();
    result.feats = columns.next();
    result.total = Float.parseFloat(columns.next());
    result.origRank = origRank;
    result.metricRank = new int[numMetrics];

    return result;
  }

  public String toString(String[] metricNames, String[] submetricNames) {

    StringBuilder rankStr = new StringBuilder("origRank=" + origRank);
    for(int iMetric = 0; iMetric < metricNames.length; iMetric++) {
      rankStr.append(" " + metricNames[iMetric] + "Rank=" + metricRank[iMetric]);
    }

    StringBuilder metricString = new StringBuilder();
    if (metricScores != null) {
      metricString.append(" |||");

      for(int iMetric = 0; iMetric < metricNames.length; iMetric++) {
        metricString.append(" " + metricNames[iMetric] + "=" + metricScores[iMetric]);
      }
    }

    if (submetricScores != null) {

      for(int iSub = 0; iSub < submetricNames.length; iSub++) {
        metricString.append(" " + submetricNames[iSub] + "=" + submetricScores[iSub]);
      }
    }

    return String.format("%d ||| %s ||| %s ||| %f ||| %s", sentId, hyp, feats, total, rankStr.toString())
        + metricString.toString();
  }
}
