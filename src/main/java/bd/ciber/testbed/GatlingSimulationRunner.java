package bd.ciber.testbed;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GatlingSimulationRunner {
	
	@Value( "${dataFolder}" )
	private String dataFolder;
	
	@Value( "${resultsFolder}" )
	private String resultsFolder;
	
	@Value( "${requestBodiesFolder}" )
	private String requestBodiesFolder;

	@Value( "${simulation}" ) String[] simulations;
	
	public void run(String simulation) {
		// Arguments
		List<String> args = new ArrayList<String>();
		args.addAll(asList("-df", dataFolder,
		"-rf", resultsFolder,
		"-rbf", requestBodiesFolder,
		"-s", simulation));
		io.gatling.app.Gatling.runGatling(args.toArray(new String[args.size()]));
	}
}
 