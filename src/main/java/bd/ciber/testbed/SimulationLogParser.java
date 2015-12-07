package bd.ciber.testbed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
 * [scenario][userId][recordType][groupHierarchy][name][first/last byte sent timestamp][first/last byte received timestamp][status][extraInfo]
 * @author greg
 *
 */
public class SimulationLogParser {
	private static final Logger LOG = LoggerFactory.getLogger(SimulationLogParser.class);
	private static final int IDX_NAME = 0; // class of simulation or scenario name
	private static final int IDX_TASK = 2; // says "REQUEST" for HTTP requests
	private static final int IDX_SUBTASK = 3; // START/END scenarios, etc..
	private static final int IDX_REQUEST_NAME = 4; // request name
	private static final int IDX_FIRST_BYTE_SENT_TIME = 5;
	private static final int IDX_LAST_BYTE_SENT_TIME = 6;
	private static final int IDX_FIRST_BYTE_RECD_TIME = 7;
	private static final int IDX_LAST_BYTE_RECD_TIME = 8;
	private static final int IDX_REQUEST_STATUS = 9; // OK/KO
	private static final int IDX_REQUEST_ERROR = 10; // error string (on KOs)
	
	private static Map<String, Set<String>> simBrownDogRequestNames = new HashMap<String, Set<String>>();	
	private static Set<String> ignoredRequests = new HashSet<String>();
	
	static {
		ignoredRequests.add("ignore this gatling request code");
		simBrownDogRequestNames.put("default", Collections.singleton("postFile"));
		simBrownDogRequestNames.put("documents2pdfsimulation", Collections.singleton("postFile"));
		simBrownDogRequestNames.put("legacyimage2tiffsimulation", Collections.singleton("postFile"));
		simBrownDogRequestNames.put("extractcollectionsimulation", new HashSet<String>(Arrays.asList("postUrl", "pollFileId")));
	}
	
	// /simulation.log KO example (spaces added)
	// testAlloyCrawlsToData \t 4560041668039034658-0 \t REQUEST \t request_container \t 1422461824410 \t 1422461824747 \t 1422461825163 \t 1422461825163 \t KO \t error string
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parse(File simLogFile) throws IOException {
		Map<String, Long> metrics = new HashMap<String, Long>();
		List<Map<String, Object>> KOs = new ArrayList<Map<String, Object>>();
		List<String> lines = (List<String>)FileUtils.readLines(simLogFile);
		String simName = lines.get(0).split("\\t")[1];
		Set<String> metricRequestNames = simBrownDogRequestNames.get(simName);
		if(metricRequestNames == null) {
			metricRequestNames = simBrownDogRequestNames.get("default");
		}
		long totalBrownDogTime = 0;
		int totalBrownDogRequests = 0;
		long maxBrownDogTime = 0;
		long minBrownDogTime = -1;
		for(String line : lines) {
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
						long timestamp = Long.parseLong(values.get(IDX_FIRST_BYTE_SENT_TIME));
						error.put(DATETIME, new Date(timestamp));
					} else {
						String reqName = values.get(IDX_REQUEST_NAME);
						if(metricRequestNames.contains(reqName)) {
							long reqTime = calcBrownDogTime(values);
							totalBrownDogTime += reqTime;
							totalBrownDogRequests++;
							if(reqTime > maxBrownDogTime) maxBrownDogTime = reqTime;
							if(minBrownDogTime == -1 | reqTime < minBrownDogTime) minBrownDogTime = reqTime;
						}
					}
				}
			} catch(IndexOutOfBoundsException continuing) {}
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put(MongoKeys.ERRORS, KOs);

		if(totalBrownDogRequests > 0) {
			metrics.put(MongoKeys.BDMETRICS_MAX, new Long(maxBrownDogTime));
			metrics.put(MongoKeys.BDMETRICS_MIN, new Long(minBrownDogTime));
			metrics.put(MongoKeys.BDMETRICS_MEAN, new Long(totalBrownDogTime / totalBrownDogRequests));
			result.put(MongoKeys.BDMETRICS, metrics);
		}
		return result;
	}

	/**
	 * Calculates request time for metrics
	 * @param values
	 * @return
	 */
	private static long calcBrownDogTime(List<String> values) {
		long doneSending = Long.parseLong(values.get(IDX_LAST_BYTE_SENT_TIME));
		long beginReceiving = Long.parseLong(values.get(IDX_FIRST_BYTE_RECD_TIME));
		return beginReceiving - doneSending;
	}
	
	
}
