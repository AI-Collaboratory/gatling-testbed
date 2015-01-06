package bd.ciber.testbed;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.core.scenario.Simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import scala.Option;
import bd.ciber.gatling.PolyglotSimulation;
import static java.util.Arrays.asList;

public class GatlingSimulationRunner {
	
	@Value( "${dataFolder}" )
	private String dataFolder;
	
	@Value( "${resultsFolder}" )
	private String resultsFolder;
	
	@Value( "${requestBodiesFolder}" )
	private String requestBodiesFolder;

	@Value( "${simulation}" ) String[] simulations;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GatlingSimulationRunner gsr = new GatlingSimulationRunner();
		gsr.runTest();
	}
	
	public void runTest() {
		// Arguments
		List<String> args = new ArrayList<String>();
		args.addAll(asList("-df", dataFolder,
		"-rf", resultsFolder,
		"-rbf", requestBodiesFolder,
		"-s", "bd.ciber.gatling.PolyglotSimulation"));
		io.gatling.app.Gatling.runGatling(args.toArray(new String[args.size()]));
	}
}
 