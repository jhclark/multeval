package multeval;

import jannopts.ConfigurationException;
import jannopts.Configurator;

import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import multeval.metrics.BLEU;
import multeval.metrics.Length;
import multeval.metrics.Metric;
import multeval.metrics.TER;

import com.google.common.collect.ImmutableMap;

public class MultEval {

	// case sensitivity option? both? use punctuation?
	// report length!

	public static Map<String, Metric<?>> KNOWN_METRICS = ImmutableMap.<String, Metric<?>> builder()
			.put("bleu", new BLEU())
			.put("meteor", new multeval.metrics.METEOR())
			.put("ter", new TER())
			.put("length", new Length())
			.build();

	static List<Metric<?>> loadMetrics(String[] metricNames, Configurator opts)
			throws ConfigurationException {

		// 1) activate config options so that we fail-fast
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (String metricName : metricNames) {
			System.err.println("Loading metric: " + metricName);
			Metric<?> metric = KNOWN_METRICS.get(metricName.toLowerCase());
			if (metric == null) {
				throw new RuntimeException("Unknown metric: " + metricName
						+ "; Known metrics are: " + KNOWN_METRICS.keySet());
			}

			// add metric options on-the-fly as needed
			opts.activateDynamicOptions(metric.getClass());

			metrics.add(metric);
		}

		// 2) load metric resources, etc.
		for (Metric<?> metric : metrics) {
			metric.configure(opts);
		}

		return metrics;
	}

	private static final ImmutableMap<String, Module> MODULES =
			new ImmutableMap.Builder<String, Module>().put("eval", new MultEvalModule())
					.put("nbest", new NbestModule())
					.build();

	static int initThreads(final List<Metric<?>> metrics, int threads) {

		if (threads == 0) {
			threads = Runtime.getRuntime().availableProcessors();
		}
		System.err.println("Using " + threads + " threads");
		return threads;
	}

//        private static String loadVersion() throws IOException {
//            Properties props = new Properties();
//            FileInputStream in = new FileInputStream("constants");
//            props.load(in);
//            in.close();
//            String version = props.getProperty("version");
//            return version;
//        }

	public static void main(String[] args) throws ConfigurationException, IOException,
			InterruptedException {

            String version = MultEval.class.getPackage().getImplementationVersion();
            System.err.println(String.format("MultEval V%s\n", version) +
                               "By Jonathan Clark\n" +
                               "Using Libraries: METEOR (Michael Denkowski) and TER (Matthew Snover)\n");

		if (args.length == 0 || !MODULES.keySet().contains(args[0])) {
			System.err.println("Usage: program <module_name> <module_options>");
			System.err.println("Available modules: " + MODULES.keySet().toString());
			System.exit(1);
		} else {
			String moduleName = args[0];
			Module module = MODULES.get(moduleName);
			Configurator opts = new Configurator().withModuleOptions(moduleName, module.getClass());

			// add "dynamic" options, which might be activated later
			// by the specified switch values
			for (Class<?> c : module.getDynamicConfigurables()) {
				opts.allowDynamicOptions(c);
			}

			try {
				opts.readFrom(args);
				opts.configure(module);
			} catch (ConfigurationException e) {

				opts.printUsageTo(System.err);
				System.err.println("ERROR: " + e.getMessage() + "\n");
				System.exit(1);
			}

			module.run(opts);
		}
	}
}
