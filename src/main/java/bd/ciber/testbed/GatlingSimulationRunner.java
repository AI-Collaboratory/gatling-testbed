package bd.ciber.testbed;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import scala.Option;

public class GatlingSimulationRunner {
	private static final Logger LOG = LoggerFactory
			.getLogger(GatlingSimulationRunner.class);

	@Value("${dataFolder}")
	private String dataFolder;

	@Value("${resultsFolder}")
	private String resultsFolder;

	@Value("${bodiesFolder}")
	private String bodiesFolder;

	
	List<String> simulations;

	public List<String> getSimulations() {
		return simulations;
	}

	public void setSimulations(List<String> simulations) {
		this.simulations = simulations;
	}

	@Autowired
	private MongoResultsCollector mongoResultsCollector;

	public void run(String simulation) throws ClassNotFoundException {
		// Put parameters into System properties
		Properties testprops = new Properties();
		try {
			testprops.load(GatlingSimulationRunner.class
					.getResourceAsStream("/testbed.properties"));
			for (Enumeration<Object> keys = testprops.keys(); keys
					.hasMoreElements();) {
				String key = (String) keys.nextElement();
				if (key.startsWith(simulation)) {
					String simkey = key.substring(simulation.length()+1);
					System.setProperty(simkey, testprops.getProperty(key));
				}
			}
		} catch (IOException e) {
			throw new Error(e);
		}

		// Arguments
		Class c = GatlingSimulationRunner.class.getClassLoader().loadClass(
				simulation);
		Option simul = Option.apply(c);
		List<String> args = new ArrayList<String>();
		args.addAll(asList(
				"-m", // mute prompts
				"-df", dataFolder, "-rf", resultsFolder, "-bdf", bodiesFolder,
				"-s", simulation));
		LOG.debug("Running Gatling: " + args.toString());
		io.gatling.app.Gatling.runGatling(
				args.toArray(new String[args.size()]), simul);
	}

	// @Scheduled(cron="*/10 * * * * *")
	// every half-hour
	//@Scheduled(initialDelay = 30000, fixedDelay = 1800000)
	public void runAll() throws InterruptedException {
		boolean first = true;
		int count = 0;
		for (String simulation : this.simulations) {
			if (first) {
				first = false;
			} else {
				Thread.sleep(5 * 60 * 1000);
			}
			try {
				count++;
				run(simulation);
				mongoResultsCollector.collectLatest(1);
			} catch (ClassNotFoundException e) {
				LOG.error("Cannot find simulation class", e);
			}
		}
		//mongoResultsCollector.collectLatest(count);
	}
}