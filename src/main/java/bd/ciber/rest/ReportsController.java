package bd.ciber.rest;

import java.io.File;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Produces a sorted manifest of available for each simulation.
 * @author greg
 *
 */
@RestController
@RequestMapping("/reports/")
public class ReportsController {
	private static final Logger LOG = LoggerFactory.getLogger(ReportsController.class);
	
	private static DateFormat FORMAT = SimpleDateFormat.getDateInstance();

	@Value( "${resultsFolder}" )
	String resultsFolder;
	
	@RequestMapping(method = RequestMethod.GET,headers="Accept=application/json")
	public Map<String, List<Map<String, String>>> listReports() {
		Map<String, List<Map<String, String>>> result = new HashMap<String, List<Map<String, String>>>();
		File dir = new File(this.resultsFolder);
		for(File entry : dir.listFiles()) {
			if(!entry.isDirectory()) continue;
			String simulationName = entry.getName().substring(0, entry.getName().indexOf("-"));
			String timestamp = entry.getName().substring(entry.getName().indexOf("-")+1, entry.getName().length());
			long ts = Long.parseLong(timestamp);
			Date dt = new Date(ts);
			String formattedDate = FORMAT.format(dt);
			Map<String, String> myreport = new HashMap<String, String>();
			myreport.put("url", "/testbed/results/"+entry.getName()+"/index.html");
			myreport.put("timestamp", timestamp);
			myreport.put("date", formattedDate);
			List<Map<String, String>> reports = null;
			if(!result.containsKey(simulationName)) {
				reports = new ArrayList<Map<String, String>>();
				result.put(simulationName, reports);
			} else {
				reports = result.get(simulationName);
			}
			reports.add(myreport);
		}

		for(List<Map<String, String>> entry : result.values()) {
			Collections.sort(entry, new Comparator<Map<String, String>>() {
				@Override
				public int compare(Map<String, String> arg0, Map<String, String> arg1) {
					String val0 = arg0.get("timestamp");
					String val1 = arg1.get("timestamp");
					return Collator.getInstance().compare(val0, val1);
				}
			});
		}
		
		return result;
	}
}
