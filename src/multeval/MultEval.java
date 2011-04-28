package multeval;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import multeval.ResultsManager.Type;
import multeval.metrics.BLEU;
import multeval.metrics.METEOR;
import multeval.metrics.Metric;
import multeval.metrics.TER;
import multeval.output.LatexTable;
import multeval.significance.BootstrapResampler;
import multeval.significance.StratifiedApproximateRandomizationTest;
import multeval.util.LibUtil;
import multeval.util.MathUtils;
import multeval.util.SuffStatUtils;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;
import jannopts.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MultEval {

	// case sensitivity option? both? use punctuation?
	// report length!

	public static Map<String, Metric> KNOWN_METRICS = ImmutableMap.<String, Metric> builder()
			.put("bleu", new BLEU())
			.put("meteor", new METEOR())
			.put("ter", new TER())
			.build();

	public static interface Module {

		public Iterable<Class<?>> getConfigurables();

		public void run(Configurator opts) throws ConfigurationException;
	}

	public static class MultEvalModule implements Module {

		@Option(shortName = "v", longName = "verbosity", usage = "Verbosity level", defaultValue = "0")
		public int verbosity;

		@Option(shortName = "o", longName = "metrics", usage = "Space-delimited list of metrics to use. Any of: bleu, meteor, ter, length", defaultValue = "bleu meteor ter", arrayDelim = " ")
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

		// TODO: Lowercasing option
		// TODO: Output index of median system according to each metric

		@Override
		public Iterable<Class<?>> getConfigurables() {
			return ImmutableList.<Class<?>> of(MultEval.class, BLEU.class, METEOR.class, TER.class);
		}

		@Override
		public void run(Configurator opts) throws ConfigurationException {

			List<Metric> metrics = loadMetrics(metricNames, opts);
			
			// 1) load hyps and references
			// first index is opt run, second is hyp
			String[][] hypFilesBySysSplit = new String[hypFilesBySys.length][];
			for (int i = 0; i < hypFilesBySys.length; i++) {
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
			// TODO: Eventually multi-thread this... but TER isn't threadsafe
			SuffStatManager suffStats = collectSuffStats(metrics, data);

			String[] metricNames = new String[metrics.size()];
			for (int i = 0; i < metricNames.length; i++) {
				metricNames[i] = metrics.get(i).toString();
			}
			String[] sysNames = new String[data.getNumSystems()];
			sysNames[0] = "baseline";
			for (int i = 1; i < sysNames.length; i++) {
				sysNames[i] = "system " + i;
			}
			ResultsManager results = new ResultsManager(metricNames, sysNames);

			// 3) evaluate each system and report the average scores
			runOverallEval(metrics, data, suffStats, results);

			// 4) run bootstrap resampling for each system, for each
			// optimization run
			runBootstrapResampling(metrics, data, suffStats, results);

			// 5) run AR -- FOR EACH SYSTEM PAIR
			runApproximateRandomization(metrics, data, suffStats, results);

			// 6) output pretty table
			LatexTable table = new LatexTable();
			table.write(results, new PrintWriter(System.out));

			// 7) show statistics such as most frequent OOV's length, brevity
			// penalty, etc.
		}

		private List<Metric> loadMetrics(String[] metricNames, Configurator opts) throws ConfigurationException {

			// TODO: Check only for selected metrics
			LibUtil.checkLibrary("jbleu.JBLEU", "jBLEU");
			LibUtil.checkLibrary("edu.cmu.meteor.scorer.MeteorScorer", "METEOR");
			System.err.println("Using METEOR Version " + edu.cmu.meteor.util.Constants.VERSION);
			LibUtil.checkLibrary("ter.TERpara", "TER");

			List<Metric> metrics = new ArrayList<Metric>();
			for (String metricName : metricNames) {
				System.err.println("Loading metric: " + metricName);
				Metric metric = KNOWN_METRICS.get(metricName.toLowerCase());
				if (metric == null) {
					throw new RuntimeException("Unknown metric: " + metricName
							+ "; Known metrics are: " + KNOWN_METRICS.keySet());
				}
				
				// add metric options on-the-fly as needed
				opts.useDynamicOptions(metric.getClass());
				
				metric.configure(opts);
				metrics.add(metric);
			}
			return metrics;
		}

		private void runApproximateRandomization(List<Metric> metrics, HypothesisManager data,
				SuffStatManager suffStats, ResultsManager results) {

			int iBaselineSys = 0;
			for (int iSys = 1; iSys < data.getNumSystems(); iSys++) {

				// index 1: metric, index 2: hypothesis, inner array: suff stats
				List<List<float[]>> suffStatsBaseline =
						suffStats.getStatsAllOptForSys(iBaselineSys);
				List<List<float[]>> suffStatsSysI = suffStats.getStatsAllOptForSys(iSys);

				StratifiedApproximateRandomizationTest ar =
						new StratifiedApproximateRandomizationTest(metrics, suffStatsBaseline,
								suffStatsSysI);
				double[] pByMetric = ar.getTwoSidedP(numShuffles);
				for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
					results.report(iMetric, iSys, Type.P_VALUE, pByMetric[iMetric]);
				}
			}
		}

		private SuffStatManager collectSuffStats(List<Metric> metrics, HypothesisManager data) {
			SuffStatManager suffStats =
					new SuffStatManager(metrics.size(), data.getNumSystems(), data.getNumOptRuns(),
							data.getNumHyps());

			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				Metric metric = metrics.get(iMetric);
				System.err.println("Collecting sufficient statistics for metric: "
						+ metric.toString());

				for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
					for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
						for (int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
							String hyp = data.getHypothesis(iSys, iOpt, iHyp);
							List<String> refs = data.getReferences(iHyp);
							float[] stats = metric.stats(hyp, refs);
							suffStats.saveStats(iMetric, iSys, iOpt, iHyp, stats);
						}
					}
				}
			}
			return suffStats;
		}

		private void runOverallEval(List<Metric> metrics, HypothesisManager data,
				SuffStatManager suffStats, ResultsManager results) {
			for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
				Metric metric = metrics.get(iMetric);
				System.err.println("Scoring with metric: " + metric.toString());

				for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {
					double[] scoresByOptRun = new double[data.getNumOptRuns()];
					for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
						List<float[]> stats = suffStats.getStats(iMetric, iSys, iOpt);
						float[] sumStats = SuffStatUtils.sumStats(stats);
						scoresByOptRun[iOpt] = metric.score(sumStats);
					}
					double avg = MathUtils.average(scoresByOptRun);
					double stddev = MathUtils.stddev(scoresByOptRun);
					double min = MathUtils.min(scoresByOptRun);
					double max = MathUtils.max(scoresByOptRun);

					results.report(iMetric, iSys, Type.AVG, avg);
					results.report(iMetric, iSys, Type.STDDEV, stddev);
					results.report(iMetric, iSys, Type.MIN, min);
					results.report(iMetric, iSys, Type.MAX, max);
				}
			}
		}

		private void runBootstrapResampling(List<Metric> metrics, HypothesisManager data,
				SuffStatManager suffStats, ResultsManager results) {
			for (int iSys = 0; iSys < data.getNumSystems(); iSys++) {

				double[] meanByMetric = new double[metrics.size()];
				double[] stddevByMetric = new double[metrics.size()];
				double[] minByMetric = new double[metrics.size()];
				double[] maxByMetric = new double[metrics.size()];

				for (int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {

					System.err.println("Performing bootstrap resampling to estimate stddev for test set selection (System "
							+ (iSys + 1) + " of " + data.getNumSystems() + ")");

					// index 1: metric, index 2: hypothesis, inner array: suff
					// stats
					List<List<float[]>> suffStatsSysI = suffStats.getStats(iSys, iOpt);
					BootstrapResampler boot = new BootstrapResampler(metrics, suffStatsSysI);
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

	private static final ImmutableMap<String, Module> modules =
			new ImmutableMap.Builder<String, Module>().put("eval", new MultEvalModule()).build();

	public static void main(String[] args) throws ConfigurationException {

		System.err.println("WARNING: THIS SOFTWARE IS STILL UNDER TESTING. PLEASE DO NOT REPORT ANY RESULTS COMPUTED BY THIS CODE. TESTING WILL BE COMPLETED NO LATER THAN MAY 1, 2011.");

		if (args.length == 0 || !modules.keySet().contains(args[0])) {
			System.err.println("Usage: program <module_name> <module_options>");
			System.err.println("Available modules: " + modules.keySet().toString());
			System.exit(1);
		} else {
			String moduleName = args[0];
			Module module = modules.get(moduleName);
			Configurator opts =
					new Configurator().withProgramHeader(
							"MultEval V0.1\nBy Jonathan Clark\nUsing Libraries: METEOR (Michael Denkowski) and TER (Matthew Snover)\n")
							.withModuleOptions(moduleName, module.getClass());

			
			
			try {
				opts.readFrom(args);
				opts.configure(module);
			} catch (ConfigurationException e) {
				
				// show options for all potential metrics on failure
				for (Class<?> configurable : module.getConfigurables()) {
					opts.withModuleOptions(moduleName, configurable);
				}
				
				opts.printUsageTo(System.err);
				System.err.println("ERROR: " + e.getMessage() + "\n");
				System.exit(1);
			}
			
			module.run(opts);
		}
	}
}
