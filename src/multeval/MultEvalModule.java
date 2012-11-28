package multeval;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;
import jannopts.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import multeval.ResultsManager.Type;
import multeval.analysis.DiffRanker;
import multeval.analysis.SentFormatter;
import multeval.metrics.BLEU;
import multeval.metrics.METEORStats;
import multeval.metrics.Metric;
import multeval.metrics.SuffStats;
import multeval.metrics.TER;
import multeval.output.AsciiTable;
import multeval.output.LatexTable;
import multeval.parallel.MetricWorkerPool;
import multeval.significance.BootstrapResampler;
import multeval.significance.StratifiedApproximateRandomizationTest;
import multeval.util.CollectionUtils;
import multeval.util.MathUtils;
import multeval.util.SuffStatUtils;
import multeval.util.Triple;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class MultEvalModule implements Module {

	@Option(shortName = "v", longName = "verbosity", usage = "Verbosity level (Integer: 0-1)", defaultValue = "0")
	public int verbosity;

	@Option(shortName = "o", longName = "metrics", usage = "Space-delimited list of metrics to use. Any of: bleu, meteor, ter, length", defaultValue = "bleu meteor ter length", arrayDelim = " ")
	public String[] metricNames;

	@Option(shortName = "B", longName = "hyps-baseline", usage = "Space-delimited list of files containing tokenized, fullform hypotheses, one per line", arrayDelim = " ")
	public String[] hypFilesBaseline;

	// each element of the array is a system that the user designated with a
	// number. each string element contains a space-delimited list of
	// hypothesis files with each file containing hypotheses from one
	// optimizer run
	@Option(shortName = "H", longName = "hyps-sys", usage = "Space-delimited list of files containing tokenized, fullform hypotheses, one per line", arrayDelim = " ", numberable = true)
	public String[] hypFilesBySys;

	@Option(shortName = "R", longName = "refs", usage = "Space-delimited list of files containing tokenized, fullform references, one per line", arrayDelim = " ")
	public String[] refFiles;

	@Option(shortName = "b", longName = "boot-samples", usage = "Number of bootstrap replicas to draw during bootstrap resampling to estimate standard deviation for each system", defaultValue = "10000")
	private int numBootstrapSamples;

	@Option(shortName = "s", longName = "ar-shuffles", usage = "Number of shuffles to perform to estimate p-value during approximate randomization test system *PAIR*", defaultValue = "10000")
	private int numShuffles;

	@Option(shortName = "L", longName = "latex", usage = "Latex-formatted table including measures that are commonly (or should be commonly) reported", required = false)
	private String latexOutFile;

	@Option(shortName = "F", longName = "fullLatexDoc", usage = "Output a fully compilable Latex document instead of just the table alone", required = false, defaultValue = "false")
	private boolean fullLatexDoc;

	@Option(shortName = "r", longName = "rankDir", usage = "Rank hypotheses of median optimization run of each system with regard to improvement/decline over median baseline system and output to the specified directory for analysis", required = false)
	private String rankDir;

	@Option(shortName = "S", longName = "sentLevelDir", usage = "Score the hypotheses of each system at the sentence-level and output to the specified directory for analysis", required = false)
	private String sentLevelDir;

	@Option(shortName = "D", longName = "debug", usage = "Show debugging output?", required = false, defaultValue = "false")
	private boolean debug;

	@Option(shortName = "t", longName = "threads", usage = "How many threads should we use? Thread-unsafe metrics will be run in a separate thread. (Zero means all available cores)", required = false, defaultValue = "0")
	private int threads;

	// TODO: Lowercasing option

	@Override
	public Iterable<Class<?>> getDynamicConfigurables() {
		return ImmutableList.<Class<?>> of(BLEU.class, multeval.metrics.METEOR.class, TER.class);
	}

	@Override
	public void run(Configurator opts) throws ConfigurationException, FileNotFoundException,
			InterruptedException {

		List<Metric<?>> metrics = MultEval.loadMetrics(metricNames, opts);
		
		this.threads = MultEval.initThreads(metrics, threads);

		// 1) load hyps and references
		// first index is opt run, second is hyp
		int numSystems = hypFilesBySys == null ? 0 : hypFilesBySys.length;
		String[][] hypFilesBySysSplit = new String[numSystems][];
		for (int i = 0; i < numSystems; i++) {
			hypFilesBySysSplit[i] = StringUtils.split(hypFilesBySys[i], " ", Integer.MAX_VALUE);
		}

		HypothesisManager data = new HypothesisManager();
		try {
			data.loadData(hypFilesBaseline, hypFilesBySysSplit, refFiles);
		} catch (IOException e) {
			System.err.println("Error while loading data.");
			e.printStackTrace();
			System.exit(1);
		}

		// 2) collect sufficient stats for each metric selected
		Stopwatch watch = new Stopwatch();
		watch.start();
		SuffStatManager suffStats = collectSuffStats(metrics, data);
		watch.stop();
		System.err.println("Collected suff stats in " + watch.toString(3));

		String[] metricNames = new String[metrics.size()];
		for (int i = 0; i < metricNames.length; i++) {
			metricNames[i] = metrics.get(i).toString();
		}
		String[] sysNames = new String[data.getNumSystems()];
		sysNames[0] = "baseline";
		for (int i = 1; i < sysNames.length; i++) {
			sysNames[i] = "system " + i;
		}
		ResultsManager results =
				new ResultsManager(metricNames, sysNames, data.getNumOptRuns());

		// 3) evaluate each system and report the average scores
		runOverallEval(metrics, data, suffStats, results);
		runOOVAnalysis(metrics, data, suffStats, results);

                // output sentence-level scores, if requested
                runSentScores(metrics, data, suffStats, results);

		// run diff ranking, if requested (MUST be run after overall eval,
		// which computes median systems)
		runDiffRankEval(metrics, data, suffStats, results);

		// 4) run bootstrap resampling for each system, for each
		// optimization run
		watch.reset();
		watch.start();
		runBootstrapResampling(metrics, data, suffStats, results);
		watch.stop();
		System.err.println("Performed bootstrap resampling in " + watch.toString(3));

		// 5) run AR -- FOR EACH SYSTEM PAIR
		watch.reset();
		watch.start();
		runApproximateRandomization(metrics, data, suffStats, results);
		watch.stop();
		if(data.getNumSystems() > 1) {
			System.err.println("Performed approximate randomization in " + watch.toString(3));
		}

		// 6) output pretty table
		if (latexOutFile != null) {
			LatexTable table = new LatexTable();
			File file = new File(latexOutFile);
			System.err.println("Writing Latex table to " + file.getAbsolutePath());
			PrintWriter out = new PrintWriter(file);
			table.write(results, metrics, out, fullLatexDoc);
			out.close();
		}

		AsciiTable table = new AsciiTable();
		table.write(results, System.out);

		// 7) show statistics such as most frequent OOV's length, brevity
		// penalty, etc.
	}

        private void runSentScores(List<Metric<?>> metrics, HypothesisManager data, SuffStatManager suffStats, ResultsManager results) throws FileNotFoundException {
            if (sentLevelDir != null) {
                File dir = new File(sentLevelDir);
                dir.mkdirs();
                System.err.println("Outputting sentence level scores to: " + dir.getAbsolutePath());
                
		final String[] submetricNames = NbestModule.getSubmetricNames(metrics);
                SentFormatter formatter = new SentFormatter(metricNames, submetricNames);
                List<List<String>> refs = data.getAllReferences();

                for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
                    for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
                        File outFile = new File(dir, String.format("sys%d.opt%d", (iSys + 1), (iOpt+1)));
                        double[][] sentMetricScores = getSentLevelScores(metrics, data, suffStats, iSys, iOpt);
                        double[][] sentSubmetricScores = getSentLevelSubmetricScores(metrics, data, suffStats, iSys, iOpt);
                        List<String> hyps = data.getHypotheses(iSys, iOpt);
                        
                        PrintWriter out = new PrintWriter(outFile);
                        formatter.write(hyps, refs, sentMetricScores, sentSubmetricScores, out);
                        out.close();
                    }
                }
            }
        }

	private void runDiffRankEval(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, ResultsManager results) throws FileNotFoundException {

		if (rankDir != null) {

			File rankOutDir = new File(rankDir);
			rankOutDir.mkdirs();
			System.err.println("Outputting ranked hypotheses to: "
					+ rankOutDir.getAbsolutePath());

			DiffRanker ranker = new DiffRanker(metricNames);
			List<List<String>> refs = data.getAllReferences();

			int iBaselineSys = 0;
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				int iBaselineMedianIdx =
						results.get(iMetric, iBaselineSys, Type.MEDIAN_IDX).intValue();
				List<String> hypsMedianBaseline =
						data.getHypotheses(iBaselineSys, iBaselineMedianIdx);

				// we must always recalculate all metric scores since
				// the median system might change based on which metric
				// we're sorting by
				double[][] sentMetricScoresBaseline =
						getSentLevelScores(metrics, data, suffStats, iBaselineSys,
								iBaselineMedianIdx);

				for (int iSys = 1; iSys < data.getNumSystems(); iSys++) {
					File outFile =
							new File(rankOutDir, String.format("sys%d.sortedby.%s", (iSys + 1),
									metricNames[iMetric]));

					int iSysMedianIdx = results.get(iMetric, iSys, Type.MEDIAN_IDX).intValue();

					List<String> hypsMedianSys = data.getHypotheses(iSys, iSysMedianIdx);

					double[][] sentMetricScoresSys =
							getSentLevelScores(metrics, data, suffStats, iSys, iSysMedianIdx);

					PrintWriter out = new PrintWriter(outFile);
					ranker.write(hypsMedianBaseline, hypsMedianSys, refs,
							sentMetricScoresBaseline, sentMetricScoresSys, iMetric, out);
					out.close();
				}
			}
		}
	}

	private double[][] getSentLevelSubmetricScores(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, int iSys, int iOpt) {

            int nSubmetrics = 0;
            for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
                nSubmetrics += metrics.get(iMetric).getSubmetricNames().length;
            }
            
            double[][] result = new double[data.getNumHyps()][nSubmetrics];
            for (int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
                int iSubmetric = 0;
                for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
                    Metric<?> metric = metrics.get(iMetric);
                    SuffStats<?> stats = suffStats.getStats(iMetric, iSys, iOpt, iHyp);
                    for (double sub : metric.scoreSubmetricsStats(stats)) {
                        result[iHyp][iSubmetric] = sub;
                        iSubmetric++;
                    }
                }
            }
            return result;
        }

	private double[][] getSentLevelScores(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, int iSys, int iOpt) {

		double[][] result = new double[data.getNumHyps()][metrics.size()];
		for (int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {

				Metric<?> metric = metrics.get(iMetric);
				SuffStats<?> stats = suffStats.getStats(iMetric, iSys, iOpt, iHyp);
				result[iHyp][iMetric] = metric.scoreStats(stats);

				// System.err.println("hyp " + (iHyp + 1) + ": " +
				// result[iHyp][iMetric]);
			}

		}
		return result;
	}

	private void runApproximateRandomization(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, ResultsManager results) throws InterruptedException {

		int iBaselineSys = 0;
		for (int iSys = 1; iSys < data.getNumSystems(); iSys++) {

			System.err.println("Performing approximate randomization to estimate p-value between baseline system and system "
					+ (iSys + 1) + " (of " + data.getNumSystems() + ")");

			// index 1: metric, index 2: hypothesis, inner array: suff stats
			List<List<SuffStats<?>>> suffStatsBaseline =
					suffStats.getStatsAllOptForSys(iBaselineSys);
			List<List<SuffStats<?>>> suffStatsSysI = suffStats.getStatsAllOptForSys(iSys);

			StratifiedApproximateRandomizationTest ar =
					new StratifiedApproximateRandomizationTest(threads, metrics, suffStatsBaseline,
							suffStatsSysI, data.getNumHyps(), data.getNumOptRuns(), debug);
			double[] pByMetric = ar.getTwoSidedP(numShuffles);
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				results.report(iMetric, iSys, Type.P_VALUE, pByMetric[iMetric]);
			}
		}
	}

	private SuffStatManager collectSuffStats(List<Metric<?>> metrics,
			final HypothesisManager data) throws InterruptedException {

		final SuffStatManager suffStats =
				new SuffStatManager(metrics.size(), data.getNumSystems(), data.getNumOptRuns(),
						data.getNumHyps());

		// parallelize at the hypothesis level
		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			final int iMetricF = iMetric;
			final Metric<?> metricMaster = metrics.get(iMetric);

			System.err.println("Collecting sufficient statistics for metric: "
					+ metricMaster.toString());
			MetricWorkerPool<Triple<Integer, Integer, Integer>, Metric<?>> work =
					new MetricWorkerPool<Triple<Integer, Integer, Integer>, Metric<?>>(
							threads, new Supplier<Metric<?>>() {
								@Override
								public Metric<?> get() {
									return metricMaster.threadClone();
								}
							}) {

						@Override
						public void doWork(Metric<?> metricCopy,
								Triple<Integer, Integer, Integer> trip) {
							int iSys = trip.first;
							int iOpt = trip.second;
							int iHyp = trip.third;

							String hyp = data.getHypothesis(iSys, iOpt, iHyp);
							List<String> refs = data.getReferences(iHyp);
							SuffStats<?> stats = metricCopy.stats(hyp, refs);
							suffStats.saveStats(iMetricF, iSys, iOpt, iHyp, stats);
						}
					};

			work.start();
			for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
				for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
					for (int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
						work.addTask(new Triple<Integer, Integer, Integer>(iSys, iOpt, iHyp));
					}
				}
			}
			work.waitForCompletion();

			System.err.println("Finished collecting sufficient statistics for metric: "
					+ metricMaster.toString());
		}

		return suffStats;
	}

	private void runOverallEval(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, ResultsManager results) {

		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			Metric<?> metric = metrics.get(iMetric);
			System.err.println("Scoring with metric: " + metric.toString());

			for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
				double[] scoresByOptRun = new double[data.getNumOptRuns()];
				for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
					List<SuffStats<?>> statsBySent = suffStats.getStats(iMetric, iSys, iOpt);
					SuffStats<?> corpusStats = SuffStatUtils.sumStats(statsBySent);
					scoresByOptRun[iOpt] = metric.scoreStats(corpusStats);
                                        if (verbosity >= 1) {
                                            // TODO: Logging framework rather than verbosity level
                                            System.err.println("RESULT: " + results.sysNames[iSys] + ": " + results.metricNames[iMetric] + ": OptRun " + iOpt + ": " + String.format("%.6f", scoresByOptRun[iOpt]));
                                        }
				}
				double avg = MathUtils.average(scoresByOptRun);
				double stddev = MathUtils.stddev(scoresByOptRun);
				double min = MathUtils.min(scoresByOptRun);
				double max = MathUtils.max(scoresByOptRun);
				int medianIdx = MathUtils.medianIndex(scoresByOptRun);
				double median = scoresByOptRun[medianIdx];

				results.report(iMetric, iSys, Type.AVG, avg);
				results.report(iMetric, iSys, Type.MEDIAN, median);
				results.report(iMetric, iSys, Type.STDDEV, stddev);
				results.report(iMetric, iSys, Type.MIN, min);
				results.report(iMetric, iSys, Type.MAX, max);
				results.report(iMetric, iSys, Type.MEDIAN_IDX, medianIdx);
			}
		}
	}

	private void runOOVAnalysis(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, ResultsManager results) {

		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			Metric<?> metric = metrics.get(iMetric);

			// just do this with METEOR since it's the most forgiving
			if (metric instanceof multeval.metrics.METEOR) {
				multeval.metrics.METEOR meteor = (multeval.metrics.METEOR) metric;

				for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
					Multiset<String> unmatchedHypWords = HashMultiset.create();
					Multiset<String> unmatchedRefWords = HashMultiset.create();
					int medianIdx = results.get(iMetric, iSys, Type.MEDIAN_IDX).intValue();

					List<SuffStats<?>> statsBySent =
							suffStats.getStats(iMetric, iSys, medianIdx);
					for (int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
						METEORStats sentStats = (METEORStats) statsBySent.get(iHyp);
						unmatchedHypWords.addAll(meteor.getUnmatchedHypWords(sentStats));
						unmatchedRefWords.addAll(meteor.getUnmatchedRefWords(sentStats));
					}

					// print OOVs for this system
					List<Entry<String>> unmatchedHypWordsSorted =
							CollectionUtils.sortByCount(unmatchedHypWords);
					List<Entry<String>> unmatchedRefWordsSorted =
							CollectionUtils.sortByCount(unmatchedRefWords);

					int nHead = 10;
					System.err.println("Top unmatched hypothesis words accoring to METEOR: "
							+ CollectionUtils.head(unmatchedHypWordsSorted, nHead));
					System.err.println("Top unmatched reference words accoring to METEOR: "
							+ CollectionUtils.head(unmatchedRefWordsSorted, nHead));
				}
			}
		}
	}

	private void runBootstrapResampling(List<Metric<?>> metrics, HypothesisManager data,
			SuffStatManager suffStats, ResultsManager results) throws InterruptedException {
		for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {

			double[] meanByMetric = new double[metrics.size()];
			double[] stddevByMetric = new double[metrics.size()];
			double[] minByMetric = new double[metrics.size()];
			double[] maxByMetric = new double[metrics.size()];

			for (int i = 0; i < metrics.size(); i++) {
				minByMetric[i] = Double.MAX_VALUE;
				maxByMetric[i] = Double.MIN_VALUE;
			}

			for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {

				System.err.println("Performing bootstrap resampling to estimate stddev for test set selection (System "
						+ (iSys + 1)
						+ " of "
						+ data.getNumSystems()
						+ "; opt run "
						+ (iOpt + 1) + " of " + data.getNumOptRuns() + ")");

				// index 1: metric, index 2: hypothesis, inner array: suff
				// stats
				List<List<SuffStats<?>>> suffStatsSysI = suffStats.getStats(iSys, iOpt);
				BootstrapResampler boot = new BootstrapResampler(threads, metrics, suffStatsSysI);
				List<double[]> sampledScoresByMetric = boot.resample(numBootstrapSamples);

				for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
					double[] sampledScores = sampledScoresByMetric.get(iMetric);

					double mean = MathUtils.average(sampledScores);
					double stddev = MathUtils.stddev(sampledScores);
					double min = MathUtils.min(sampledScores);
					double max = MathUtils.max(sampledScores);
					// TODO: also include 95% CI?

					meanByMetric[iMetric] += mean / data.getNumOptRuns();
					stddevByMetric[iMetric] += stddev / data.getNumOptRuns();
					minByMetric[iMetric] = Math.min(min, minByMetric[iMetric]);
					maxByMetric[iMetric] = Math.max(max, maxByMetric[iMetric]);
				}
			}

			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				results.report(iMetric, iSys, Type.RESAMPLED_MEAN_AVG, meanByMetric[iMetric]);
				results.report(iMetric, iSys, Type.RESAMPLED_STDDEV_AVG,
						stddevByMetric[iMetric]);
				results.report(iMetric, iSys, Type.RESAMPLED_MIN, minByMetric[iMetric]);
				results.report(iMetric, iSys, Type.RESAMPLED_MAX, maxByMetric[iMetric]);
			}
		}
	}
}
