package multeval.analysis;

import java.io.*;
import java.util.*;

// sentence-level analysis only
public class SentFormatter {

  private String[] metricNames;
  private String[] submetricNames;

  public SentFormatter(String[] metricNames, String[] submetricNames) {
    this.metricNames = metricNames;
    this.submetricNames = submetricNames;
  }

  private static class HypItem {
    public final int id;
    public final List<String> refs;
    public final String hyp;
    public final double[] metricScores;
    public final double[] submetricScores;

      public HypItem(int id, List<String> refs, String hyp, double[] metricScores, double[] submetricScores) {
      this.id = id;
      this.hyp = hyp;
      this.refs = refs;
      this.metricScores = metricScores;
      this.submetricScores = submetricScores;
    }
  }

  public void write(List<String> hyps, List<List<String>> refs,
                    double[][] sentMetricScores, double[][] sentSubmetricScores, PrintWriter out) {

    // merge all the data together
    List<HypItem> list = new ArrayList<HypItem>(hyps.size());
    for(int i = 0; i < hyps.size(); i++) {
        list.add(new HypItem(i, refs.get(i), hyps.get(i), sentMetricScores[i], sentSubmetricScores[i]));
    }

    // format the entries and write the file
    for(HypItem item : list) {
      // TODO: option: show "submetrics"

      out.printf("%d ||| %s |||", item.id, item.hyp);

      for(int iMetric = 0; iMetric < metricNames.length; iMetric++) {
          out.printf(" %s=%.2f", metricNames[iMetric], item.metricScores[iMetric]);
      }
      out.print(" |||");

      for(int iSubmetric = 0; iSubmetric < submetricNames.length; iSubmetric++) {
          out.printf(" %s=%.2f", submetricNames[iSubmetric], item.submetricScores[iSubmetric]);
      }

      /*
      for(int iRef = 0; iRef < item.refs.size(); iRef++) {
        out.printf("ref%d: %s", iRef + 1, item.refs.get(iRef));
        out.println();
      }
      */
      out.println();
    }
  }
}
