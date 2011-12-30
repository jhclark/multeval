package multeval;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import multeval.metrics.BLEU;
import multeval.metrics.Metric;
import multeval.metrics.SuffStats;
import multeval.metrics.TER;
import multeval.parallel.HypothesisLevelMetricWorkerPool;
import multeval.util.FileUtils;
import multeval.util.SuffStatUtils;
import multeval.util.SynchronizedBufferedReader;
import multeval.util.SynchronizedPrintStream;

import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

public class NbestModule implements Module {

	@Option(shortName = "v", longName = "verbosity", usage = "Verbosity level", defaultValue = "0")
	public int verbosity;

	@Option(shortName = "o", longName = "metrics", usage = "Space-delimited list of metrics to use. Any of: bleu, meteor, ter, length", defaultValue = "bleu meteor ter", arrayDelim = " ")
	public String[] metricNames;

	@Option(shortName = "N", longName = "nbest", usage = "File containing tokenized, fullform hypotheses, one per line")
	public String nbestList;

	@Option(shortName = "R", longName = "refs", usage = "Space-delimited list of files containing tokenized, fullform references, one per line", arrayDelim = " ")
	public String[] refFiles;

	@Option(shortName = "r", longName = "rankDir", usage = "Rank hypotheses of median optimization run of each system with regard to improvement/decline over median baseline system and output to the specified directory for analysis", required = false)
	private String rankDir;

	@Option(shortName = "t", longName = "threads", usage = "Number of threads to use. This will be reset to 1 thread if you choose to use any thread-unsafe metrics such as TER (Zero means use all available cores)", defaultValue = "0")
	private int threads;

	@Override
	public Iterable<Class<?>> getDynamicConfigurables() {
		return ImmutableList.<Class<?>> of(BLEU.class, multeval.metrics.METEOR.class, TER.class);
	}

	public static class NbestTask {
		public final List<NbestEntry> myHyps;
		public final List<String> sentRefs;

		public NbestTask(List<NbestEntry> myHyps, List<String> sentRefs) {
			this.myHyps = myHyps;
			this.sentRefs = sentRefs;
		}
	}

	@Override
	public void run(Configurator opts) throws ConfigurationException, IOException,
			InterruptedException {

		final List<Metric<?>> metrics = MultEval.loadMetrics(metricNames, opts);
		final String[] submetricNames = getSubmetricNames(metrics);

		this.threads = MultEval.initThreads(metrics, threads);

		// 1) count hyps for error checking
		String lastLine = FileUtils.getLastLine(nbestList);
		NbestEntry lastEntry = NbestEntry.parse(lastLine, -1, 0);
		int numHyps = lastEntry.sentId + 1; // zero-based

		// 2) load refs
		List<List<String>> allRefs = HypothesisManager.loadRefs(refFiles, numHyps);

		System.err.println("Found " + numHyps + " hypotheses with " + allRefs.get(0).size()
				+ " references");

		// 3) process n-best list and write results
		final SynchronizedPrintStream out = new SynchronizedPrintStream(System.out);
		final SynchronizedPrintStream[] metricRankFiles =
				rankDir == null ? null : new SynchronizedPrintStream[metrics.size()];
		;
		if (rankDir != null) {
			new File(rankDir).mkdirs();
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				metricRankFiles[iMetric] =
						new SynchronizedPrintStream(new PrintStream(new File(rankDir,
								metricNames[iMetric] + ".sorted"), "UTF-8"));
			}
		}

		SynchronizedBufferedReader in =
				new SynchronizedBufferedReader(new BufferedReader(new InputStreamReader(
						new FileInputStream(nbestList), Charsets.UTF_8)));
		String line;
		final int DEFAULT_NUM_HYPS = 1000;
		List<NbestEntry> hyps = new ArrayList<NbestEntry>(DEFAULT_NUM_HYPS);
		final List<List<SuffStats<?>>> oracleStatsByMetric =
				new ArrayList<List<SuffStats<?>>>(metrics.size());
		final List<List<SuffStats<?>>> woracleStatsByMetric =
				new ArrayList<List<SuffStats<?>>>(metrics.size());
		final List<List<SuffStats<?>>> topbestStatsByMetric =
				new ArrayList<List<SuffStats<?>>>(metrics.size());
		for (int i = 0; i < metrics.size(); i++) {
			oracleStatsByMetric.add(new ArrayList<SuffStats<?>>());
			woracleStatsByMetric.add(new ArrayList<SuffStats<?>>());
			topbestStatsByMetric.add(new ArrayList<SuffStats<?>>());
		}

		NbestModule.NbestTask poison = new NbestTask(null, null);
		HypothesisLevelMetricWorkerPool<NbestModule.NbestTask, List<Metric<?>>> work =
				new HypothesisLevelMetricWorkerPool<NbestModule.NbestTask, List<Metric<?>>>(
						threads, poison, new Supplier<List<Metric<?>>>() {
							@Override
							public List<Metric<?>> get() {
								List<Metric<?>> copy = new ArrayList<Metric<?>>(metrics.size());
								for (Metric<?> metric : metrics) {
									copy.add(metric.threadClone());
								}
								return copy;
							}
						}) {

					@Override
					public void doWork(List<Metric<?>> localMetrics, NbestModule.NbestTask t) {
						// local metrics are thread-safe on a per-instance basis
						// (i.e. multiple threads cannot access the same
						// instance)
						try {
							processHyp(localMetrics, submetricNames, t.myHyps, t.sentRefs, out,
									metricRankFiles, oracleStatsByMetric, woracleStatsByMetric,
									topbestStatsByMetric);
						} catch (InterruptedException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}

				};
		work.start();

		int curHyp = 0;
		int iLine = 0;
		while ((line = in.readLine()) != null) {
			iLine++;
			NbestEntry entry = NbestEntry.parse(line, hyps.size(), metrics.size());
			if (curHyp != entry.sentId) {
				final List<String> sentRefs = allRefs.get(curHyp);
				work.addTask(new NbestTask(hyps, sentRefs));

				if (iLine % 10000 == 0) {
					System.err.println("Processed " + iLine + " lines (" + curHyp
							+ " hypotheses) so far...");
				}

				int prevNumHyps = hyps.size(); // prevent future growing

				// don't just clear this!
				// pending tasks hold a reference to the previous instance of
				// this list, so it's important not to mutate it; we prefer not
				// to make an immutable copy since that can be expensive
				hyps = new ArrayList<NbestEntry>(prevNumHyps);
				entry.origRank = 0;
				curHyp = entry.sentId;
			}
			hyps.add(entry);
		}

		// handle last sentence
		List<String> sentRefs = allRefs.get(curHyp);
		work.addTask(new NbestTask(hyps, sentRefs));

		work.waitForCompletion();

		out.close();

		if (rankDir != null) {
			System.err.println("Wrote n-best list ranked by metrics to: " + rankDir);
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				metricRankFiles[iMetric].close();
			}
		}

		for (int i = 0; i < metrics.size(); i++) {
			Metric<?> metric = metrics.get(i);

			SuffStats<?> topbestStats = SuffStatUtils.sumStats(topbestStatsByMetric.get(i));
			double topbestScore = metric.scoreStats(topbestStats);
			String topbestSub = metric.scoreSubmetricsString(topbestStats);
			System.err.println(String.format("%s topbest score: %.2f (%s)", metric.toString(),
					topbestScore, topbestSub));

			SuffStats<?> oracleStats = SuffStatUtils.sumStats(oracleStatsByMetric.get(i));
			double oracleScore = metric.scoreStats(oracleStats);
			String oracleSub = metric.scoreSubmetricsString(oracleStats);
			System.err.println(String.format("%s oracle score: %.2f (%s)", metric.toString(),
					oracleScore, topbestSub));

			SuffStats<?> woracleStats = SuffStatUtils.sumStats(woracleStatsByMetric.get(i));
			double woracleScore = metric.scoreStats(woracleStats);
			String woracleSub = metric.scoreSubmetricsString(woracleStats);
			System.err.println(String.format("%s worst-oracle score: %.2f (%s)", metric.toString(),
					woracleScore, woracleSub));
		}
	}

	private String[] getSubmetricNames(List<Metric<?>> metrics) {
		int numSubmetrics = 0;
		for (Metric<?> metric : metrics) {
			numSubmetrics += metric.getSubmetricNames().length;
		}
		String[] submetricNames = new String[numSubmetrics];
		int i = 0;
		for (Metric<?> metric : metrics) {
			for (String name : metric.getSubmetricNames()) {
				submetricNames[i] = name;
				i++;
			}
		}
		return submetricNames;
	}

	// process all hypotheses corresponding to a single sentence
	private void processHyp(List<Metric<?>> metricCopies, String[] submetricNames,
			List<NbestEntry> hyps, List<String> sentRefs, SynchronizedPrintStream out,
			SynchronizedPrintStream[] metricRankFiles,
			List<List<SuffStats<?>>> oracleStatsByMetric,
			List<List<SuffStats<?>>> woracleStatsByMetric,
			List<List<SuffStats<?>>> topbestStatsByMetric) throws InterruptedException {

		// score all of the hypotheses in the n-best list
		for (int iRank = 0; iRank < hyps.size(); iRank++) {

			List<SuffStats<?>> metricStats = new ArrayList<SuffStats<?>>(metricCopies.size());
			double[] metricScores = new double[metricCopies.size()];
			double[] submetricScores = new double[submetricNames.length];
			NbestEntry entry = hyps.get(iRank);

			int iSubmetric = 0;
			for (int iMetric = 0; iMetric < metricCopies.size(); iMetric++) {
				Metric<?> metric = metricCopies.get(iMetric);
				SuffStats<?> stats = metric.stats(entry.hyp, sentRefs);

				metricStats.add(stats);
				metricScores[iMetric] = metric.scoreStats(stats);

				for (double sub : metric.scoreSubmetricsStats(stats)) {
					submetricScores[iSubmetric] = sub;
					iSubmetric++;
				}
			}

			entry.metricStats = metricStats;
			entry.metricScores = metricScores;
			entry.submetricScores = submetricScores;
		}

		// assign rank by each metric and save suff stats for the topbest
		// hyp
		// accoring to each metric
		for (int iMetric = 0; iMetric < metricCopies.size(); iMetric++) {

			// TODO: Should we make this a single sync block to reduce lock
			// overhead?
			synchronized (topbestStatsByMetric) {
				topbestStatsByMetric.get(iMetric).add(hyps.get(0).metricStats.get(iMetric));
			}

			sortByMetricScore(hyps, iMetric, metricCopies.get(iMetric).isBiggerBetter());
			synchronized (oracleStatsByMetric) {
				oracleStatsByMetric.get(iMetric).add(hyps.get(0).metricStats.get(iMetric));
			}
			synchronized (woracleStatsByMetric) {
				woracleStatsByMetric.get(iMetric).add(
						hyps.get(hyps.size() - 1).metricStats.get(iMetric));
			}

			// and record the rank of each
			for (int iRank = 0; iRank < hyps.size(); iRank++) {
				hyps.get(iRank).metricRank[iMetric] = iRank;
			}
		}

		// put them back in their original order
		Collections.sort(hyps, new Comparator<NbestEntry>() {
			public int compare(NbestEntry a, NbestEntry b) {
				int ra = a.origRank;
				int rb = b.origRank;
				return (ra < rb ? -1 : 1);
			}
		});

		int sentId = hyps.get(0).sentId;

		// and write them to an output file
		for (NbestEntry entry : hyps) {
			out.println(sentId, entry.toString(metricNames, submetricNames));
		}
		out.finishUnit(sentId);

		if (metricRankFiles != null) {
			for (int iMetric = 0; iMetric < metricCopies.size(); iMetric++) {
				sortByMetricScore(hyps, iMetric, metricCopies.get(iMetric).isBiggerBetter());

				// and write them to an output file
				for (NbestEntry entry : hyps) {
					metricRankFiles[iMetric].println(sentId,
							entry.toString(metricNames, submetricNames));
				}
				metricRankFiles[iMetric].finishUnit(sentId);
			}
		}
	}

	private void sortByMetricScore(List<NbestEntry> hyps, final int i, final boolean isBiggerBetter) {
		Collections.sort(hyps, new Comparator<NbestEntry>() {
			public int compare(NbestEntry a, NbestEntry b) {
				double da = a.metricScores[i];
				double db = b.metricScores[i];
				if (isBiggerBetter) {
					return (da == db ? 0 : (da > db ? -1 : 1));
				} else {
					return (da == db ? 0 : (da < db ? -1 : 1));
				}
			}
		});
	}
}
