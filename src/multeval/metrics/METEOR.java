package multeval.metrics;

import jannopts.*;

import java.io.*;
import java.net.*;
import java.util.*;

import multeval.util.*;
import com.google.common.primitives.*;
import com.google.common.collect.*;
import com.google.common.base.*;

import edu.cmu.meteor.scorer.*;
import edu.cmu.meteor.util.*;
import edu.cmu.meteor.aligner.*;

public class METEOR extends Metric<METEORStats> {

  @Option(shortName = "l", longName = "meteor.language", usage = "Two-letter language code of a supported METEOR language (e.g. 'en')")
  String language;

  @Option(shortName = "t", longName = "meteor.task", usage = "One of: rank adq hter tune (Rank is generally a good choice)", defaultValue = "rank")
  String task;

  @Option(shortName = "p", longName = "meteor.params", usage = "Custom parameters of the form 'alpha beta gamma' (overrides default)", arrayDelim = " ", required = false)
  double[] params;

  @Option(shortName = "m", longName = "meteor.modules", usage = "Specify modules. (overrides default) Any of: exact stem synonym paraphrase", arrayDelim = " ", required = false)
  String[] modules;

  @Option(shortName = "w", longName = "meteor.weights", usage = "Specify module weights (overrides default)", arrayDelim = " ", required = false)
  double[] moduleWeights;

  @Option(shortName = "x", longName = "meteor.beamSize", usage = "Specify beam size (overrides default)", defaultValue = ""
      + Constants.DEFAULT_BEAM_SIZE)
  int beamSize;

  @Option(shortName = "s", longName = "meteor.synonymDirectory", usage = "If default is not desired (NOTE: This option has a different short flag than stand-alone METEOR)", required = false)
  String synonymDirectory;

  @Option(shortName = "a", longName = "meteor.paraphraseFile", usage = "If default is not desired", required = false)
  String paraphraseFile;

  @Option(shortName = "k", longName = "meteor.keepPunctuation", usage = "Consider punctuation when aligning sentences (if false, the meteor tokenizer will be run, after which punctuation will be removed)", defaultValue = "true")
  boolean keepPunctuation;

  public static final String[] SUBMETRIC_NAMES = { "prec", "rec", "frag" };

  // TODO: Meteor normalization?

  private MeteorScorer scorer;

  @Override
  public String getMetricDescription() {
    StringBuilder builder = new StringBuilder();
    builder.append("Meteor V"+edu.cmu.meteor.util.Constants.VERSION);
    builder.append(" " + language);
    builder.append(" on " + task + " task");
    if(modules == null) {
      builder.append(" with all default modules");
    } else {
      builder.append(" with modules " + Joiner.on(", ").join(modules));
    }
    if(keepPunctuation) {
      builder.append(" NOT");
    }
    builder.append(" ignoring punctuation");
    return builder.toString();
  }

  @Override
  public void configure(Configurator opts) throws ConfigurationException {
    LibUtil.checkLibrary("edu.cmu.meteor.scorer.MeteorScorer", "METEOR");
    System.err.println("Using METEOR Version " + edu.cmu.meteor.util.Constants.VERSION);

    opts.configure(this);

    // do some sanity checking
    if(Constants.getLanguageID(Constants.normLanguageName(language)) == Constants.LANG_OTHER && Constants.getTaskID(task) != Constants.TASK_LI) {
	throw new ConfigurationException("Unrecognized METEOR language: "+language);
    }
    if(Constants.getTaskID(task) == Constants.TASK_CUSTOM) {
	throw new ConfigurationException("Unrecognized METEOR task: "+task);
    }

    MeteorConfiguration config = new MeteorConfiguration();
    config.setLanguage(language);
    // task must be set after language due to Meteor initializing defaults
    config.setTask(task);

    if (params != null) {
      config.setParameters(new ArrayList<Double>(Doubles.asList(params)));
    }

    if (moduleWeights != null) {
      config.setModuleWeights(new ArrayList<Double>(Doubles.asList(moduleWeights)));
    }

    if (modules != null) {
      List<String> moduleList = Arrays.asList(modules);
      config.setModulesByName(new ArrayList<String>(moduleList));

      // error if not enough weights
      if (config.getModuleWeights().size() != modules.length) {
        throw new RuntimeException("You provided " + modules.length + " modules and "
            + config.getModuleWeights().size() + " module weights");
      }
    }

    config.setBeamSize(beamSize);

    if (synonymDirectory != null) {
      try {
        // This should not ever throw a malformed url exception
        config.setSynDirURL((new File(synonymDirectory)).toURI().toURL());
      } catch(MalformedURLException e) {
        throw new Error(e);
      }
    }

    if (paraphraseFile != null) {
      try {
        // This should not ever throw a malformed url exception
        config.setParaFileURL((new File(paraphraseFile)).toURI().toURL());
      } catch(MalformedURLException e) {
        throw new Error(e);
      }
    }

    if (keepPunctuation) {
      config.setNormalization(Constants.NO_NORMALIZE);
    } else {
      config.setNormalization(Constants.NORMALIZE_NO_PUNCT);
    }

    System.err.println("Loading METEOR paraphrase table...");
    scorer = new MeteorScorer(config);
  }

  @Override
  public METEORStats stats(String hyp, List<String> refs) {
    // TODO: Don't create so many garbage MeteorStats objects just to be
    // copy-constructed
    MeteorStats result = scorer.getMeteorStats(hyp, new ArrayList<String>(refs));
    return new METEORStats(result);
  }

  @Override
  public double score(METEORStats suffStats) {
    scorer.computeMetrics(suffStats.meteorStats);
    return suffStats.meteorStats.score * 100;
  }

  @Override
  public double[] scoreSubmetrics(METEORStats suffStats) {
    MeteorStats stats = suffStats.meteorStats;
    scorer.computeMetrics(stats);
    return new double[] { stats.precision, stats.recall, stats.fragPenalty };
  }

  @Override
  public String[] getSubmetricNames() {
    return SUBMETRIC_NAMES;
  }

  public Multiset<String> getUnmatchedHypWords(METEORStats stats) {
  
    List<String> hypWords = stats.meteorStats.alignment.words1;
    Multiset<String> result = HashMultiset.create(hypWords);
    for(Match m : stats.meteorStats.alignment.matches) {
      if (m != null) {
	  int hypMatchStart = m.matchStart;
	  int hypMatchLen = m.matchLength;
	  for (int i = 0; i < hypMatchLen; i++) {
	      String matchedHypWord = hypWords.get(hypMatchStart + i);
	      result.remove(matchedHypWord);
	  }
      }
    }
    return result;
  }

  public Multiset<String> getUnmatchedRefWords(METEORStats stats) {

    List<String> refWords = stats.meteorStats.alignment.words2;
    Multiset<String> result = HashMultiset.create(refWords);
    for(Match m : stats.meteorStats.alignment.matches) {
	if(m != null) {
	    int refMatchStart = m.start;
	    int refMatchLen = m.length;
	    for (int i = 0; i < refMatchLen; i++) {
		String matchedRefWord = refWords.get(refMatchStart + i);
		result.remove(matchedRefWord);
	    }
	}
    }
    return result;
  }

  @Override
  public String toString() {
    return "METEOR";
  }

  @Override
  public boolean isBiggerBetter() {
    return true;
  }

  @Override
  public boolean isThreadsafe() {
    return true;
  }
  
  @Override
  public Metric<?> threadClone() {
	  METEOR metric = new METEOR();
	  metric.scorer = new MeteorScorer(scorer);
	  return metric;
  }
}
