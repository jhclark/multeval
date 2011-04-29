package multeval.analysis;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiffRanker {

	private String[] metricNames;

	public DiffRanker(String[] metricNames) {
		this.metricNames = metricNames;
	}

	private static class HypItem {
		public final int id;
		public final List<String> refs;
		public final String hypBaseline;
		public final String hypSys;
		public final double[] metricScoresBaseline;
		public final double[] metricScoresSys;

		public HypItem(int id, List<String> refs, String hypBaseline, String hypSys,
				double[] metricScoresBaseline, double[] metricScoresSys) {

			this.id = id;
			this.hypBaseline = hypBaseline;
			this.hypSys = hypSys;
			this.refs = refs;
			this.metricScoresBaseline = metricScoresBaseline;
			this.metricScoresSys = metricScoresSys;
		}

		public double diff(int iMetric) {
			return metricScoresSys[iMetric] - metricScoresBaseline[iMetric];
		}
	}

	public void write(List<String> hypsMedianBaseline, List<String> hypsMedianSys,
			List<List<String>> refs, double[][] sentMetricScoresBaseline,
			double[][] sentMetricScoresSys, final int sortByMetric, PrintWriter out) {

		// merge all the data together
		List<HypItem> list = new ArrayList<HypItem>(hypsMedianBaseline.size());
		for (int i = 0; i < hypsMedianBaseline.size(); i++) {
			list.add(new HypItem(i, refs.get(i), hypsMedianBaseline.get(i), hypsMedianSys.get(i),
					sentMetricScoresBaseline[i], sentMetricScoresSys[i]));
		}

		// sort by metric improvement/decline
		Collections.sort(list, new Comparator<HypItem>() {
			@Override
			public int compare(HypItem hypA, HypItem hypB) {
				double diffA = hypA.diff(sortByMetric);
				double diffB = hypB.diff(sortByMetric);
				// most improved first
				return -(diffA < diffB ? -1 : (diffA == diffB ? 0 : 1));
			}
		});

		// TODO: Write the median optimization run id to

		// format the entries and write the file
		for (HypItem item : list) {
			// TODO: option: show all optimization runs?
			// TODO: option: show "submetrics"

			out.printf("SENT %d\t", item.id);

			// first, write out sort-by metric
			out.printf("%s: %.1f -> %.1f = %.1f", metricNames[sortByMetric],
					item.metricScoresBaseline[sortByMetric], item.metricScoresSys[sortByMetric],
					item.diff(sortByMetric));

			for (int iMetric = 0; iMetric < metricNames.length; iMetric++) {
				if (iMetric != sortByMetric) {
					out.printf("\t%s: %.1f -> %.1f = %.1f", metricNames[iMetric],
							item.metricScoresBaseline[iMetric], item.metricScoresSys[iMetric],
							item.diff(iMetric));
				}
			}
			out.println();
			out.printf("%s-median-baseline: %s", metricNames[sortByMetric], item.hypBaseline);
			out.println();
			// TODO: Insert system names here
			out.printf("%s-median-variant: %s", metricNames[sortByMetric], item.hypSys);
			out.println();
			for (int iRef = 0; iRef < item.refs.size(); iRef++) {
				out.printf("ref%d: %s", iRef + 1, item.refs.get(iRef));
				out.println();
			}
			out.println();
		}
	}
}
