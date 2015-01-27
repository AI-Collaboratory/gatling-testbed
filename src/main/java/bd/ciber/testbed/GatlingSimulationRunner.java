package bd.ciber.testbed;

import static java.util.Arrays.asList;
import io.gatling.core.scenario.Simulation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import bd.ciber.gatling.Documents2PDFSimulation;
import scala.Option;

public class GatlingSimulationRunner {
	private static final Logger LOG = LoggerFactory.getLogger(GatlingSimulationRunner.class);
	
	@Value( "${dataFolder}" )
	private String dataFolder;
	
	@Value( "${resultsFolder}" )
	private String resultsFolder;
	
	@Value( "${bodiesFolder}" )
	private String bodiesFolder;

	@Value( "${simulations}" ) String[] simulations;
	
	public void run(String simulation) {
		// Arguments
		Option simul = Option.apply(Documents2PDFSimulation.class);
		List<String> args = new ArrayList<String>();
		args.addAll(asList("-m", // mute prompts
		"-df", dataFolder,
		"-rf", resultsFolder,
		"-bdf", bodiesFolder,
		"-s", simulation));
		LOG.debug("Running Gatling: "+args.toString());
		io.gatling.app.Gatling.runGatling(args.toArray(new String[args.size()]), simul);
	}
	
	//@Scheduled(cron="*/10 * * * * *")
	@Scheduled(initialDelay=30000, fixedDelay=300000)
	public void runAll() throws InterruptedException {
		LOG.debug("invoked");
		boolean first = true;
		for(String simulation : this.simulations) {
			if(first) {
				first = false;
			} else {
				Thread.sleep(5*60*1000);
			}
			run(simulation);
		}
	}
}
 