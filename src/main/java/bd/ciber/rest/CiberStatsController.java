package bd.ciber.rest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.AggregationOutput;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

import static bd.ciber.testbed.CiberIndexKeys.*;

/**
 * Produces a sorted manifest of available reports for each simulation.
 * 
 * @author greg
 *
 */
@RestController
@RequestMapping("/ciberStats/")
public class CiberStatsController {
	private static final Logger LOG = LoggerFactory
			.getLogger(CiberStatsController.class);

	@Value("${cacheFolder}")
	private String cacheFolder;
	
	private String formatDistFilename = "formats.json";

	@RequestMapping(value = "/formats", method = RequestMethod.GET, headers = "Accept=application/json")
	public void formatDist(
			@RequestParam(defaultValue = "false", required = false) String reload,
			Writer responseWriter) throws IOException {
		File formatDistFile = new File(cacheFolder, formatDistFilename);
		if ("true".equals(reload) || !formatDistFile.exists()) {
			MongoClient mongoClient;
			try {
				mongoClient = new MongoClient();
			} catch (UnknownHostException e) {
				throw new Error(e);
			}
			DB testbedDB = mongoClient.getDB(DB.key());
			DBCollection coll = testbedDB.getCollection(FILES_COLL.key());

			List<DBObject> agg = new ArrayList<DBObject>();
			DBObject count = (DBObject) JSON
					.parse("{ $group: { _id: \"$extension\", count: { $sum: 1 } } }");
			DBObject moreThanAFew = (DBObject) JSON
					.parse("{ $match: { count: { $gte: 950 } } }");
			DBObject sort = (DBObject) JSON.parse("{ $sort: { count: -1 } }");
			agg.add(count);
			agg.add(moreThanAFew);
			agg.add(sort);

			AggregationOutput res = coll.aggregate(agg);

			StringWriter sw = new StringWriter();
			sw.write("{ \"formats\":[");
			boolean first = true;
			for (DBObject o : res.results()) {
				if (first) {
					first = false;
				} else {
					sw.write(',');
				}
				sw.write(JSON.serialize(o));
			}
			sw.write("] }");
			FileUtils.writeStringToFile(formatDistFile, sw.toString());
		}
		String formatDist = FileUtils.readFileToString(formatDistFile);
		responseWriter.write(formatDist);
	}

}
