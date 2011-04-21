package metrics;

import java.io.File;

import jannopts.Option;

public class METEOR implements Metric {
	
	@Option(shortName = "l", longName = "meteor.language", usage = "Two-letter language code of a supported METEOR language (e.g. 'en')")
	String lang;
	
	@Option(shortName = "t", longName = "meteor.task", usage = "One of: rank adq hter tune (Rank is generally a good choice)", defaultValue="rank")
	String task;
	
	@Option(shortName = "p", longName = "meteor.params", usage = "Custom parameters of the form 'alpha beta gamma' (overrides default)", arrayDelim=" ")
	float[] params;
	
	@Option(shortName = "m", longName = "meteor.modules", usage = "Specify modules. (overrides default) Any of: exact stem synonym paraphrase", arrayDelim=" ")
	String[] modules;
	
	@Option(shortName = "w", longName = "meteor.weights", usage = "Specify module weights (overrides default)", arrayDelim=" ")
	float[] weights;

	@Option(shortName = "x", longName = "meteor.beamSize", usage = "Specify beam size (overrides default)", defaultValue="40")
	int beamSize;
	
	@Option(shortName = "s", longName = "meteor.synonymDirectory", usage = "If default is not desired (NOTE: This option has a different short flag than stand-alone METEOR)", defaultValue="")
	String synonymDirectory;
	
	@Option(shortName = "a", longName = "meteor.paraphraseFile", usage = "If default is not desired", defaultValue="")
	String paraphraseFile;
	
	@Option(shortName = "k", longName = "meteor.keepPunctuation", usage = "Consider punctuation when aligning sentences", defaultValue="true")
	boolean keepPunctuation;

	@Override
	public void stats(String[] sentence, String[][] refs, float[] result) {
		// TODO Auto-generated method stub
	}

	@Override
	public double score(double[] suffStats) {
		// TODO Auto-generated method stub
		return 0;
	}
}
