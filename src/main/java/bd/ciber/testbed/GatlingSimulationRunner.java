package bd.ciber.testbed;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import scala.Option;

public class GatlingSimulationRunner {
	private static final Logger LOG = LoggerFactory
			.getLogger(GatlingSimulationRunner.class);
	
	private String currentlyRunningSimulation = "None";

	@Value("${dataFolder}")
	private String dataFolder;

	@Value("${resultsFolder}")
	private String resultsFolder;

	@Value("${bodiesFolder}")
	private String bodiesFolder;
	
	@Resource(name = "simulationProperties")
	private Properties simulationProperties;
	
	List<String> simulations;

	public List<String> getSimulations() {
		return simulations;
	}

	public void setSimulations(List<String> simulations) {
		this.simulations = simulations;
	}
	
	public String getCurrentRunningSimulation() {
		return this.currentlyRunningSimulation;
	}

	@Autowired
	private MongoResultsCollector mongoResultsCollector;

	public void run(String simulation) throws ClassNotFoundException {
		LOG.info("SIMULATION START: {}", simulation);
		// Put simulation properties into System properties
		for (Enumeration<Object> keys = simulationProperties.keys(); 
				keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			System.setProperty(key, simulationProperties.getProperty(key));
		}

		Class c = GatlingSimulationRunner.class.getClassLoader().loadClass(
				simulation);
		Option simul = Option.apply(c);
		List<String> args = new ArrayList<String>();
		args.addAll(asList(
				"-m", // mute prompts
				"-df", dataFolder, "-rf", resultsFolder, "-bdf", bodiesFolder,
				"-s", simulation));
		LOG.debug("Running Gatling: " + args.toString());
		try {
			io.gatling.app.Gatling.runGatling(
				args.toArray(new String[args.size()]), simul);
		} catch(RuntimeException e) {
			LOG.error("Simulation {} caused a runtime exception", simulation, e);
		} catch(Exception e) {
			LOG.error("Simulation {} threw an exception", simulation, e);
		}
		LOG.info("SIMULATION START: {}", simulation);
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
	}
}