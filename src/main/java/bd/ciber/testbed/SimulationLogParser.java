package bd.ciber.testbed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static bd.ciber.testbed.MongoKeys.*;

/**
 * Parses a simulation.log from a Gatling run, extracting KOs and other request information.
 * 
 * @author greg
 *
 */
public class SimulationLogParser {
	private static final Logger LOG = LoggerFactory.getLogger(SimulationLogParser.class);
	private static final int IDX_NAME = 0; // class of simulation or scenario name
	private static final int IDX_TASK = 2; // says "REQUEST" for HTTP requests
	private static final int IDX_SUBTASK = 3; // START/END scenarios, etc..
	private static final int IDX_REQUEST_NAME = 4; // request name
	private static final int IDX_REQUEST_TIME = 5; // request time
	private static final int IDX_REQUEST_STATUS = 9; // OK/KO
	private static final int IDX_REQUEST_ERROR = 10; // error string (on KOs)
	
	private static Set<String> ignoredRequests = new HashSet<String>();
	
	static {
		ignoredRequests.add("ignore this gatling request code");
	}
	
	// /simulation.log KO example (spaces added)
	// testAlloyCrawlsToData \t 4560041668039034658-0 \t REQUEST \t request_container \t 1422461824410 \t 1422461824747 \t 1422461825163 \t 1422461825163 \t KO \t error string
	
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> parse(File simLogFile) throws IOException {
		List<Map<String, Object>> KOs = new ArrayList<Map<String, Object>>();
		for(String line : (List<String>)FileUtils.readLines(simLogFile)) {
			List<String> values = Arrays.asList(line.split("\\t")); // split on TAB
			try {
				if("REQUEST".equals(values.get(IDX_TASK))) {
					if("KO".equals(values.get(IDX_REQUEST_STATUS))) {
						String reqName = values.get(IDX_REQUEST_NAME);
						if(ignoredRequests.contains(reqName)) continue;
						Map<String, Object> error = new HashMap<String, Object>();
						KOs.add(error);
						error.put(ERROR_MESSAGE, values.get(IDX_REQUEST_ERROR));
						error.put(ERROR_REQUEST_NAME, reqName);
						long timestamp = Long.parseLong(values.get(IDX_REQUEST_TIME));
						error.put(DATETIME, new Date(timestamp));
					}
				}
			} catch(IndexOutOfBoundsException continuing) {}
		}
		return KOs;
	}
	
	
}
