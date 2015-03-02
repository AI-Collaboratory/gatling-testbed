package bd.ciber.testbed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * Parses a simulation.log from a Gatling run, extracting KOs and other request information.
 * 
 * @author greg
 *
 */
public class SimulationLogParser {
	private static final int IDX_NAME = 0; // class of simulation or scenario name
	private static final int IDX_TASK = 2; // says "REQUEST" for HTTP requests
	private static final int IDX_SUBTASK = 3; // START/END scenarios, etc..
	private static final int IDX_REQUEST_NAME = 4; // request name
	private static final int IDX_REQUEST_STATUS = 9; // OK/KO
	private static final int IDX_REQUEST_ERROR = 10; // error string (on KOs)
	
	
	private static Set<String> ignoredRequests = new HashSet<String>();
	
	static {
		ignoredRequests.add("ignore this gatling request code");
	}
	
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> parse(File simLogFile) throws IOException {
		List<Map<String, Object>> KOs = new ArrayList<Map<String, Object>>();
		for(String line : (List<String>)FileUtils.readLines(simLogFile)) {
			String[] values = line.split("\\t"); // split on TAB
			if("REQUEST".equals(values[IDX_TASK])) {
				if(values.length > IDX_REQUEST_STATUS
						&& "KO".equals(values[IDX_REQUEST_STATUS])) {
					// TODO log this error
				}
			}
		}
		return KOs;
	}
}
