package multeval;

import multeval.metrics.BLEU;
import multeval.metrics.METEOR;
import multeval.metrics.TER;
import multeval.util.LibUtil;

import jannopts.ConfigurationException;
import jannopts.Configurator;
import jannopts.Option;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MultEval {
	
	// case sensitivity option? both? use punctuation?
	// report length!
	
	public static interface Module {

		public Iterable<Class<?>> getConfigurables();

		public void run();
	}
	
	public static class MultEvalModule implements Module {
		
		@Option(shortName = "v", longName = "verbosity", usage = "Verbosity level", defaultValue = "0")
		public int verbosity;
		
		@Option(shortName = "o", longName = "metrics", usage = "Space-delimited list of metrics to use. Any of: bleu, meteor, ter, length", defaultValue = "bleu meteor ter length", arrayDelim=" ")
		public String[] metrics;
		
		@Option(shortName = "H", longName = "hyps", usage = "Space-delimited list of files containing tokenized, fullform hypotheses, one per line", arrayDelim=" ")
		public String[] hyps;
		
		@Option(shortName = "R", longName = "refs", usage = "Space-delimited list of files containing tokenized, fullform references, one per line", arrayDelim=" ")
		public String[] refs;

		@Override
		public Iterable<Class<?>> getConfigurables() {
			 return ImmutableList.<Class<?>>of(MultEval.class, BLEU.class, METEOR.class, TER.class);
		}

		@Override
		public void run() {
			LibUtil.checkLibrary("jbleu.JBLEU", "jBLEU");
			LibUtil.checkLibrary("edu.cmu.meteor.scorer.MeteorScorer", "METEOR");
			LibUtil.checkLibrary("TERpara", "TER");
		}
		
	}
	
	private static final ImmutableMap<String, Module> modules = new ImmutableMap.Builder<String, Module>()
		.put("eval", new MultEvalModule())
		.build();
	
	public static void main(String[] args) {
		
		System.err.println("WARNING: THIS SOFTWARE IS STILL UNDER TESTING. PLEASE DO NOT REPORT ANY RESULTS COMPUTED BY THIS CODE. TESTING WILL BE COMPLETED NO LATER THAN MAY 1, 2011.");
		
		if (args.length == 0 || !modules.keySet().contains(args[0])) {
			System.err.println("Usage: program <module_name> <module_options>");
			System.err.println("Available modules: "
					+ modules.keySet().toString());
			System.exit(1);
		} else {
			String moduleName = args[0];
			Module module = modules.get(moduleName);
			Configurator opts = new Configurator().withProgramHeader(
					"MultEval V0.1\nBy Jonathan Clark\nUsing Libraries: METEOR (Michael Denkowski) and TER (Matthew Snover)\n")
					.withModuleOptions(moduleName, module.getClass());
			
			for (Class<?> configurable : module.getConfigurables()) {
				opts.withModuleOptions(moduleName, configurable);
			}

			try {
				opts.readFrom(args);
				opts.configure(module);
			} catch (ConfigurationException e) {
				opts.printUsageTo(System.err);
				System.err.println("ERROR: " + e.getMessage() + "\n");
				System.exit(1);
			}
			
			module.run();
		}
	}
}
