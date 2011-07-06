package multeval;

import jannopts.*;
import jannopts.util.StringUtils;

import java.io.*;
import java.util.*;

import multeval.ResultsManager.Type;
import multeval.analysis.*;
import multeval.metrics.*;
import multeval.output.*;
import multeval.significance.*;
import multeval.util.*;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.collect.Multiset.Entry;

public class MultEval {

  // case sensitivity option? both? use punctuation?
  // report length!

  public static Map<String, Metric<?>> KNOWN_METRICS = ImmutableMap.<String, Metric<?>> builder()
      .put("bleu", new BLEU()).put("meteor", new multeval.metrics.METEOR()).put("ter", new TER()).put("length", new Length()).build();

  private static List<Metric<?>> loadMetrics(String[] metricNames, Configurator opts) throws ConfigurationException {

    // 1) activate config options so that we fail-fast
    List<Metric<?>> metrics = new ArrayList<Metric<?>>();
    for(String metricName : metricNames) {
      System.err.println("Loading metric: " + metricName);
      Metric<?> metric = KNOWN_METRICS.get(metricName.toLowerCase());
      if (metric == null) {
        throw new RuntimeException("Unknown metric: " + metricName + "; Known metrics are: " + KNOWN_METRICS.keySet());
      }

      // add metric options on-the-fly as needed
      opts.activateDynamicOptions(metric.getClass());

      metrics.add(metric);
    }

    // 2) load metric resources, etc.
    for(Metric<?> metric : metrics) {
      metric.configure(opts);
    }

    return metrics;
  }

  public static interface Module {

    public void run(Configurator opts) throws ConfigurationException, FileNotFoundException, IOException;

    public Iterable<Class<?>> getDynamicConfigurables();
  }

  public static class NbestModule implements Module {

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

    @Override
    public Iterable<Class<?>> getDynamicConfigurables() {
      return ImmutableList.<Class<?>> of(BLEU.class, multeval.metrics.METEOR.class, TER.class);
    }

    @Override
    public void run(Configurator opts) throws ConfigurationException, IOException {

      List<Metric<?>> metrics = loadMetrics(metricNames, opts);
      String[] submetricNames = getSubmetricNames(metrics);

      // 1) count hyps for error checking
      String lastLine = FileUtils.getLastLine(nbestList);
      NbestEntry lastEntry = NbestEntry.parse(lastLine, -1, 0);
      int numHyps = lastEntry.sentId + 1; // zero-based

      // 2) load refs
      List<List<String>> allRefs = HypothesisManager.loadRefs(refFiles, numHyps);

      System.err.println("Found " + numHyps + " hypotheses with " + allRefs.get(0).size() + " references");

      // 3) process n-best list and write results
      PrintStream out = System.out;
      PrintWriter[] metricRankFiles = null;
      if (rankDir != null) {
        new File(rankDir).mkdirs();
        metricRankFiles = new PrintWriter[metrics.size()];
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          metricRankFiles[iMetric] = new PrintWriter(new File(rankDir, metricNames[iMetric] + ".sorted"));
        }
      }

      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(nbestList), Charsets.UTF_8));
      String line;
      List<NbestEntry> hyps = new ArrayList<NbestEntry>(1000);
      List<List<SuffStats<?>>> oracleStatsByMetric = new ArrayList<List<SuffStats<?>>>(metrics.size());
      List<List<SuffStats<?>>> topbestStatsByMetric = new ArrayList<List<SuffStats<?>>>(metrics.size());
      for(int i=0;i<metrics.size(); i++) {
        oracleStatsByMetric.add(new ArrayList<SuffStats<?>>());
        topbestStatsByMetric.add(new ArrayList<SuffStats<?>>());
      }
      
      int curHyp = 0;
      while((line = in.readLine()) != null) {
        NbestEntry entry = NbestEntry.parse(line, hyps.size(), metrics.size());
        if (curHyp != entry.sentId) {
          System.err.println("hyp #"+curHyp);
          List<String> sentRefs = allRefs.get(curHyp);
          processHyp(metrics, submetricNames, hyps, sentRefs, out, metricRankFiles, oracleStatsByMetric, topbestStatsByMetric);

          if (curHyp % 100 == 0) {
            System.err.println("Processed " + curHyp + " hypotheses so far...");
          }

          hyps.clear();
          curHyp = entry.sentId;
        }
        hyps.add(entry);
      }

      List<String> sentRefs = allRefs.get(curHyp);
      processHyp(metrics, submetricNames, hyps, sentRefs, out, metricRankFiles, oracleStatsByMetric, topbestStatsByMetric);

      out.close();
      if (rankDir != null) {
        System.err.println("Wrote n-best list ranked by metrics to: " + rankDir);
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          metricRankFiles[iMetric].close();
        }
      }
      
      for(int i=0; i<metrics.size(); i++) {
        Metric<?> metric = metrics.get(i);
        
        SuffStats<?> topbestStats = SuffStatUtils.sumStats(topbestStatsByMetric.get(i));
        double topbestScore = metric.scoreStats(topbestStats);
        System.err.println(String.format("%s topbest score: %.2f", metric.toString(), topbestScore));
        
        SuffStats<?> oracleStats = SuffStatUtils.sumStats(oracleStatsByMetric.get(i));
        double oracleScore = metric.scoreStats(oracleStats);
        System.err.println(String.format("%s oracle score: %.2f", metric.toString(), oracleScore));
      }
    }

    private String[] getSubmetricNames(List<Metric<?>> metrics) {
      int numSubmetrics = 0;
      for(Metric<?> metric : metrics) {
        numSubmetrics += metric.getSubmetricNames().length;
      }
      String[] submetricNames = new String[numSubmetrics];
      int i = 0;
      for(Metric<?> metric : metrics) {
        for(String name : metric.getSubmetricNames()) {
          submetricNames[i] = name;
          i++;
        }
      }
      return submetricNames;
    }

    // process all hypotheses corresponding to a single sentence
    private void processHyp(List<Metric<?>> metrics, String[] submetricNames, List<NbestEntry> hyps,
        List<String> sentRefs, PrintStream out, PrintWriter[] metricRankFiles,
        List<List<SuffStats<?>>> oracleStatsByMetric, List<List<SuffStats<?>>> topbestStatsByMetric) {

      // score all of the hypotheses in the n-best list
      for(int iRank = 0; iRank < hyps.size(); iRank++) {
        
        List<SuffStats<?>> metricStats = new ArrayList<SuffStats<?>>(metrics.size());
        double[] metricScores = new double[metrics.size()];
        double[] submetricScores = new double[submetricNames.length];
        NbestEntry entry = hyps.get(iRank);

        int iSubmetric = 0;
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          Metric<?> metric = metrics.get(iMetric);
          SuffStats<?> stats = metric.stats(entry.hyp, sentRefs);
          
          metricStats.add(stats);
          metricScores[iMetric] = metric.scoreStats(stats);

          for(double sub : metric.scoreSubmetricsStats(stats)) {
            submetricScores[iSubmetric] = sub;
            iSubmetric++;
          }
        }
        
        entry.metricStats = metricStats;
        entry.metricScores = metricScores;
        entry.submetricScores = submetricScores;
      }

      // assign rank by each metric and save suff stats for the topbest hyp
      // accoring to each metric
      for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
        
        topbestStatsByMetric.get(iMetric).add(hyps.get(0).metricStats.get(iMetric));
        
        sortByMetricScore(hyps, iMetric, metrics.get(iMetric).isBiggerBetter());
        oracleStatsByMetric.get(iMetric).add(hyps.get(0).metricStats.get(iMetric));
        
        // and record the rank of each
        for(int iRank = 0; iRank < hyps.size(); iRank++) {
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

      // and write them to an output file
      for(NbestEntry entry : hyps) {
        out.println(entry.toString(metricNames, submetricNames));
      }

      if (metricRankFiles != null) {
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          sortByMetricScore(hyps, iMetric, metrics.get(iMetric).isBiggerBetter());

          // and write them to an output file
          for(NbestEntry entry : hyps) {
            metricRankFiles[iMetric].println(entry.toString(metricNames, submetricNames));
          }
        }
      }
    }

    private void sortByMetricScore(List<NbestEntry> hyps, final int i, final boolean isBiggerBetter) {
      Collections.sort(hyps, new Comparator<NbestEntry>() {
        public int compare(NbestEntry a, NbestEntry b) {
          double da = a.metricScores[i];
          double db = b.metricScores[i];
          if(isBiggerBetter) {
            return (da == db ? 0 : (da > db ? -1 : 1));
          } else {
            return (da == db ? 0 : (da < db ? -1 : 1));
          }
        }
      });
    }
  }

  public static class MultEvalModule implements Module {

    @Option(shortName = "v", longName = "verbosity", usage = "Verbosity level", defaultValue = "0")
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

    @Option(shortName = "r", longName = "rankDir", usage = "Rank hypotheses of median optimization run of each system with regard to improvement/decline over median baseline system and output to the specified directory for analysis", required = false)
    private String rankDir;
    
    @Option(shortName = "D", longName = "debug", usage = "Show debugging output?", required = false, defaultValue="false")
    private boolean debug;

    // TODO: Lowercasing option

    @Override
    public Iterable<Class<?>> getDynamicConfigurables() {
      return ImmutableList.<Class<?>> of(BLEU.class, multeval.metrics.METEOR.class, TER.class);
    }

    @Override
    public void run(Configurator opts) throws ConfigurationException, FileNotFoundException {

      List<Metric<?>> metrics = loadMetrics(metricNames, opts);

      // 1) load hyps and references
      // first index is opt run, second is hyp
      int numSystems = hypFilesBySys == null ? 0 : hypFilesBySys.length;
      String[][] hypFilesBySysSplit = new String[numSystems][];
      for(int i = 0; i < numSystems; i++) {
        hypFilesBySysSplit[i] = StringUtils.split(hypFilesBySys[i], " ", Integer.MAX_VALUE);
      }

      HypothesisManager data = new HypothesisManager();
      try {
        data.loadData(hypFilesBaseline, hypFilesBySysSplit, refFiles);
      } catch(IOException e) {
        System.err.println("Error while loading data.");
        e.printStackTrace();
        System.exit(1);
      }

      // 2) collect sufficient stats for each metric selected
      // TODO: Eventually multi-thread this... but TER isn't threadsafe
      SuffStatManager suffStats = collectSuffStats(metrics, data);

      String[] metricNames = new String[metrics.size()];
      for(int i = 0; i < metricNames.length; i++) {
        metricNames[i] = metrics.get(i).toString();
      }
      String[] sysNames = new String[data.getNumSystems()];
      sysNames[0] = "baseline";
      for(int i = 1; i < sysNames.length; i++) {
        sysNames[i] = "system " + i;
      }
      ResultsManager results = new ResultsManager(metricNames, sysNames, data.getNumOptRuns());

      // 3) evaluate each system and report the average scores
      runOverallEval(metrics, data, suffStats, results);
      runOOVAnalysis(metrics, data, suffStats, results);

      // run diff ranking, if requested (MUST be run after overall eval,
      // which computes median systems)
      runDiffRankEval(metrics, data, suffStats, results);

      // 4) run bootstrap resampling for each system, for each
      // optimization run
      runBootstrapResampling(metrics, data, suffStats, results);

      // 5) run AR -- FOR EACH SYSTEM PAIR
      runApproximateRandomization(metrics, data, suffStats, results);

      // 6) output pretty table
      if (latexOutFile != null) {
        LatexTable table = new LatexTable();
        File file = new File(latexOutFile);
        System.err.println("Writing Latex table to " + file.getAbsolutePath());
        PrintWriter out = new PrintWriter(file);
        table.write(results, out);
        out.close();
      }
      
      AsciiTable table = new AsciiTable();
      table.write(results, System.out);

      // 7) show statistics such as most frequent OOV's length, brevity
      // penalty, etc.
    }

    private void runDiffRankEval(List<Metric<?>> metrics, HypothesisManager data, SuffStatManager suffStats,
        ResultsManager results) throws FileNotFoundException {

      if (rankDir != null) {

        File rankOutDir = new File(rankDir);
        rankOutDir.mkdirs();
        System.err.println("Outputting ranked hypotheses to: " + rankOutDir.getAbsolutePath());

        DiffRanker ranker = new DiffRanker(metricNames);
        List<List<String>> refs = data.getAllReferences();

        int iBaselineSys = 0;
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          int iBaselineMedianIdx = results.get(iMetric, iBaselineSys, Type.MEDIAN_IDX).intValue();
          List<String> hypsMedianBaseline = data.getHypotheses(iBaselineSys, iBaselineMedianIdx);

          // we must always recalculate all metric scores since
          // the median system might change based on which metric
          // we're sorting by
          double[][] sentMetricScoresBaseline = getSentLevelScores(metrics, data, suffStats, iBaselineSys,
              iBaselineMedianIdx);

          for(int iSys = 1; iSys < data.getNumSystems(); iSys++) {
            File outFile = new File(rankOutDir, String.format("sys%d.sortedby.%s", (iSys + 1), metricNames[iMetric]));

            int iSysMedianIdx = results.get(iMetric, iSys, Type.MEDIAN_IDX).intValue();

            List<String> hypsMedianSys = data.getHypotheses(iSys, iSysMedianIdx);

            double[][] sentMetricScoresSys = getSentLevelScores(metrics, data, suffStats, iSys, iSysMedianIdx);

            PrintWriter out = new PrintWriter(outFile);
            ranker.write(hypsMedianBaseline, hypsMedianSys, refs, sentMetricScoresBaseline, sentMetricScoresSys,
                iMetric, out);
            out.close();
          }
        }
      }
    }

    private double[][] getSentLevelScores(List<Metric<?>> metrics, HypothesisManager data, SuffStatManager suffStats,
        int iSys, int iOpt) {

      double[][] result = new double[data.getNumHyps()][metrics.size()];
      for(int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {

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
        SuffStatManager suffStats, ResultsManager results) {

      int iBaselineSys = 0;
      for(int iSys = 1; iSys < data.getNumSystems(); iSys++) {

        System.err
            .println("Performing approximate randomization to estimate p-value between baseline system and system "
                + (iSys + 1) + " (of " + data.getNumSystems() + ")");

        // index 1: metric, index 2: hypothesis, inner array: suff stats
        List<List<SuffStats<?>>> suffStatsBaseline = suffStats.getStatsAllOptForSys(iBaselineSys);
        List<List<SuffStats<?>>> suffStatsSysI = suffStats.getStatsAllOptForSys(iSys);

        StratifiedApproximateRandomizationTest ar = new StratifiedApproximateRandomizationTest(metrics,
            suffStatsBaseline, suffStatsSysI, data.getNumHyps(), data.getNumOptRuns(), debug);
        double[] pByMetric = ar.getTwoSidedP(numShuffles);
        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          results.report(iMetric, iSys, Type.P_VALUE, pByMetric[iMetric]);
        }
      }
    }

    private SuffStatManager collectSuffStats(List<Metric<?>> metrics, HypothesisManager data) {
      SuffStatManager suffStats = new SuffStatManager(metrics.size(), data.getNumSystems(), data.getNumOptRuns(),
          data.getNumHyps());

      for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
        Metric<?> metric = metrics.get(iMetric);
        System.err.println("Collecting sufficient statistics for metric: " + metric.toString());

        for(int iSys = 0; iSys < data.getNumSystems(); iSys++) {
          for(int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
            for(int iHyp = 0; iHyp < data.getNumHyps(); iHyp++) {
              String hyp = data.getHypothesis(iSys, iOpt, iHyp);
              List<String> refs = data.getReferences(iHyp);
              SuffStats<?> stats = metric.stats(hyp, refs);
              suffStats.saveStats(iMetric, iSys, iOpt, iHyp, stats);
            }
          }
        }
      }
      return suffStats;
    }

    private void runOverallEval(List<Metric<?>> metrics, HypothesisManager data, SuffStatManager suffStats,
        ResultsManager results) {

      for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
        Metric<?> metric = metrics.get(iMetric);
        System.err.println("Scoring with metric: " + metric.toString());

        for(int iSys = 0; iSys < data.getNumSystems(); iSys++) {
          double[] scoresByOptRun = new double[data.getNumOptRuns()];
          for(int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {
            List<SuffStats<?>> statsBySent = suffStats.getStats(iMetric, iSys, iOpt);
            SuffStats<?> corpusStats = SuffStatUtils.sumStats(statsBySent);
            scoresByOptRun[iOpt] = metric.scoreStats(corpusStats);
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

      for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
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

    private void runBootstrapResampling(List<Metric<?>> metrics, HypothesisManager data, SuffStatManager suffStats,
        ResultsManager results) {
      for(int iSys = 0; iSys < data.getNumSystems(); iSys++) {

        double[] meanByMetric = new double[metrics.size()];
        double[] stddevByMetric = new double[metrics.size()];
        double[] minByMetric = new double[metrics.size()];
        double[] maxByMetric = new double[metrics.size()];

	for(int i=0; i<metrics.size(); i++) {
	  minByMetric[i] = Double.MAX_VALUE;
	  maxByMetric[i] = Double.MIN_VALUE;
	}

        for(int iOpt = 0; iOpt < data.getNumOptRuns(); iOpt++) {

          System.err.println("Performing bootstrap resampling to estimate stddev for test set selection (System "
              + (iSys + 1) + " of " + data.getNumSystems() + "; opt run " + (iOpt + 1) + " of " + data.getNumOptRuns()
              + ")");

          // index 1: metric, index 2: hypothesis, inner array: suff
          // stats
          List<List<SuffStats<?>>> suffStatsSysI = suffStats.getStats(iSys, iOpt);
          BootstrapResampler boot = new BootstrapResampler(metrics, suffStatsSysI);
          List<double[]> sampledScoresByMetric = boot.resample(numBootstrapSamples);

          for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
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

        for(int iMetric = 0; iMetric < metrics.size(); iMetric++) {
          results.report(iMetric, iSys, Type.RESAMPLED_MEAN_AVG, meanByMetric[iMetric]);
          results.report(iMetric, iSys, Type.RESAMPLED_STDDEV_AVG, stddevByMetric[iMetric]);
          results.report(iMetric, iSys, Type.RESAMPLED_MIN, minByMetric[iMetric]);
          results.report(iMetric, iSys, Type.RESAMPLED_MAX, maxByMetric[iMetric]);
        }
      }
    }
  }

  private static final ImmutableMap<String, Module> MODULES = new ImmutableMap.Builder<String, Module>()
      .put("eval", new MultEvalModule()).put("nbest", new NbestModule()).build();

  public static void main(String[] args) throws ConfigurationException, IOException {

    if (args.length == 0 || !MODULES.keySet().contains(args[0])) {
      System.err.println("Usage: program <module_name> <module_options>");
      System.err.println("Available modules: " + MODULES.keySet().toString());
      System.exit(1);
    } else {
      String moduleName = args[0];
      Module module = MODULES.get(moduleName);
      Configurator opts = new Configurator().withProgramHeader(
          "MultEval V0.2\nBy Jonathan Clark\nUsing Libraries: METEOR (Michael Denkowski) and TER (Matthew Snover)\n")
          .withModuleOptions(moduleName, module.getClass());

      // add "dynamic" options, which might be activated later
      // by the specified switch values
      for(Class<?> c : module.getDynamicConfigurables()) {
        opts.allowDynamicOptions(c);
      }

      try {
        opts.readFrom(args);
        opts.configure(module);
      } catch(ConfigurationException e) {

        opts.printUsageTo(System.err);
        System.err.println("ERROR: " + e.getMessage() + "\n");
        System.exit(1);
      }

      module.run(opts);
    }
  }
}
