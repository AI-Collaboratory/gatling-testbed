package bd.ciber.rest;

import static bd.ciber.testbed.MongoKeys.*;

import java.io.IOException;
import java.io.Writer;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

/**
 * Produces a sorted manifest of available reports for each simulation.
 * 
 * @author greg
 *
 */
@RestController
@RequestMapping("/reports/")
public class ReportsController {
	private static final Logger LOG = LoggerFactory
			.getLogger(ReportsController.class);

	private static DateFormat FORMAT = SimpleDateFormat.getDateTimeInstance();

	@RequestMapping(method = RequestMethod.GET, headers = "Accept=application/json")
	public void listReports(@RequestParam(defaultValue="all", required=false) String sim,
			Writer responseWriter) {
		Map<String, List<Map<String, String>>> result = new HashMap<String, List<Map<String, String>>>();
		MongoClient mongoClient;
		try {
			mongoClient = new MongoClient();
		} catch (UnknownHostException e) {
			throw new Error(e);
		}
		DB testbedDB = mongoClient.getDB(DBNAME);
		DBCollection results = testbedDB.getCollection(SIMULATION_RESULTS_COLL);
		BasicDBObject ref = new BasicDBObject();
		if(sim != null && !"all".equals(sim)) {
			ref.append(SIMULATION_NAME, sim);
		}
		BasicDBObject keys = new BasicDBObject(DATETIME, true);
		keys.append(SIMULATION_NAME, true);
		keys.append(ERRORS, true);
		keys.append(GATLING_RESULTS_FOLDER, true);
		DBCursor cursor = results.find(ref, keys).sort(
				new BasicDBObject(DATETIME, -1));
		try {
			responseWriter.write("{ \"simulation-results\":[");
			boolean first = true;
			for (DBObject o : cursor) {
				if(first) {
					first = false;
				} else {
					responseWriter.write(',');
				}
				responseWriter.write(JSON.serialize(o));
			}
			responseWriter.write("] }");
		} catch (IOException e) {
			throw new Error("Cannot write response", e);
		}
	}
}
