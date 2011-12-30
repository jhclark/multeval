package multeval;

import jannopts.ConfigurationException;
import jannopts.Configurator;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface Module {

	public void run(Configurator opts) throws ConfigurationException, FileNotFoundException,
			IOException, InterruptedException;

	public Iterable<Class<?>> getDynamicConfigurables();
}