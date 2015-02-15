package bd.ciber.testbed;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import scala.Option;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class GatlingSimulationRunner {
	private static final Logger LOG = LoggerFactory
			.getLogger(GatlingSimulationRunner.class);

	@Value("${dataFolder}")
	private String dataFolder;

	@Value("${resultsFolder}")
	private String resultsFolder;

	@Value("${bodiesFolder}")
	private String bodiesFolder;

	@Value("${simulations}")
	String[] simulations;

	@Autowired
	private GatlingResults gatlingResults;

	public void run(String simulation) throws ClassNotFoundException {
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
	@Scheduled(initialDelay = 30000, fixedDelay = 300000)
	public void runAll() throws InterruptedException {
		boolean first = true;
		for (String simulation : this.simulations) {
			if (first) {
				first = false;
			} else {
				Thread.sleep(5 * 60 * 1000);
			}
			try {
				run(simulation);
				gatlingResults.collect(simulation);
			} catch (ClassNotFoundException e) {
				LOG.error("Cannot find simulation class", e);
			}
		}
	}