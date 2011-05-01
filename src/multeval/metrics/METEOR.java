package multeval.metrics;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import multeval.util.LibUtil;

import com.google.common.primitives.Doubles;

import edu.cmu.meteor.scorer.MeteorConfiguration;
import edu.cmu.meteor.scorer.MeteorScorer;
import edu.cmu.meteor.scorer.MeteorStats;
import edu.cmu.meteor.util.Constants;

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

	@Option(shortName = "x", longName = "meteor.beamSize", usage = "Specify beam size (overrides default)", defaultValue = ""+Constants.DEFAULT_BEAM_SIZE)
	int beamSize;

	@Option(shortName = "s", longName = "meteor.synonymDirectory", usage = "If default is not desired (NOTE: This option has a different short flag than stand-alone METEOR)", required = false)
	String synonymDirectory;

	@Option(shortName = "a", longName = "meteor.paraphraseFile", usage = "If default is not desired", required = false)
	String paraphraseFile;

	@Option(shortName = "k", longName = "meteor.keepPunctuation", usage = "Consider punctuation when aligning sentences (if false, the meteor tokenizer will be run, after which punctuation will be removed)", defaultValue = "true")
	boolean keepPunctuation;

	// TODO: Meteor normalization?

	private MeteorScorer scorer;

	@Override
	public void configure(Configurator opts) throws ConfigurationException {
		LibUtil.checkLibrary("edu.cmu.meteor.scorer.MeteorScorer", "METEOR");
		System.err.println("Using METEOR Version " + edu.cmu.meteor.util.Constants.VERSION);

		opts.configure(this);

		MeteorConfiguration config = new MeteorConfiguration();
		config.setLanguage(language);
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
			} catch (MalformedURLException e) {
				throw new Error(e);
			}
		}

		if (paraphraseFile != null) {
			try {
				// This should not ever throw a malformed url exception
				config.setParaFileURL((new File(paraphraseFile)).toURI().toURL());
			} catch (MalformedURLException e) {
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
	public String toString() {
		return "METEOR";
	}
}
