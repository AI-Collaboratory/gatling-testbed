package bd.ciber.testbed;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class MongoResultsCollector {
	private static final Logger LOG = LoggerFactory
			.getLogger(MongoResultsCollector.class);

	public static final String DBNAME = "testbed";
	public static final String GLOBALSTATS_COLL = "global_stats";
	public static final String REQUEST_KO_COLL = "request_ko";
	
	@Value("${resultsFolder}")
	private String resultsFolder;
	
	@PostConstruct
	public void init() throws UnknownHostException {
		// Create mongo collections
		MongoClient mongoClient = new MongoClient();
		DB testbedDB = mongoClient.getDB(DBNAME);
		Set<String> colls = testbedDB.getCollectionNames();
		if (colls.contains(GLOBALSTATS_COLL))
			testbedDB.createCollection(GLOBALSTATS_COLL, null);
		if (colls.contains(REQUEST_KO_COLL))
			testbedDB.createCollection(REQUEST_KO_COLL, null);
	}

	public File[] getLatestResultsFolders(int howMany) {
		File dir = new File(this.resultsFolder);
		SortFilter filter = new SortFilter(howMany);
		dir.listFiles(filter);
		File[] topFiles = new File[filter.topFiles.size()];
		return filter.topFiles.toArray(topFiles);
	}

	/**
	 * Collect the results of the latest N simulations into MongoDB. Will
	 * check for prior collection of each simulation.
	 * 
	 * @param foldersToExamine looks at the latest N folders
	 */
	public void collectLatest(int foldersToExamine) {
		File[] resFolders = getLatestResultsFolders(foldersToExamine);
		for(File resFolder : resFolders) {
			String fldNm = resFolder.getName();
			String simName = fldNm.substring(0, fldNm.indexOf('-')).toLowerCase();

			// See if collection contains the resFolder yet?
			collectGlobalStats(resFolder, simName);
			// TODO add KOs from simulation.log
		}
	}

	private void collectGlobalStats(File resFolder, String simName) {
		try {
			MongoClient mongoClient = new MongoClient();
			DB testbedDB = mongoClient.getDB("testbed");
			DBCollection stats = testbedDB.getCollection(GLOBALSTATS_COLL);
			// check that results folder is not already collected
			DBObject simRef = new BasicDBObject("resultsFolder", resFolder.getName());
			DBCursor cur = stats.find(simRef);
			if(cur.count() > 0) {
				LOG.debug("Already logged the results in {}", resFolder.getName());
				return;
			}
			
			// add js/global_stats.json to document
			File globalStatsFile = new File(resFolder,
					"js/global_stats.json");
			String statsStr = FileUtils.readFileToString(globalStatsFile);
			DBObject statsObject = (DBObject) JSON.parse(statsStr);
			// add timestamp
			Date modified = new Date(resFolder.lastModified());
			statsObject.put("datetime", modified);
			statsObject.put("simulation", simName);
			statsObject.put("resultsFolder", resFolder.getName());
			stats.insert(statsObject);
		} catch (UnknownHostException e) {
			LOG.error("Unexpected", e);
			throw new Error(e);
		} catch (IOException e) {
			LOG.error("Unexpected", e);
			throw new Error(e);
		}
	}

	// /simulation.log KO example
	// testAlloyCrawlsToData 4560041668039034658-0 REQUEST request_container
	// 1422461824410 1422461824747 1422461825163 1422461825163 KO error string
	
	
	public class SortFilter implements FileFilter {
		final LinkedList<File> topFiles;
		private final int n;

		public SortFilter(int n) {
			this.n = n;
			topFiles = new LinkedList<File>();
		}

		public boolean accept(File newF) {
			long newT = newF.lastModified();

			if (topFiles.size() == 0) {
				// list is empty, so we can add this one for sure
				topFiles.add(newF);
			} else {
				int limit = topFiles.size() < n ? topFiles.size() : n;
				// find a place to insert
				int i = 0;
				while (i < limit && newT <= topFiles.get(i).lastModified())
					i++;

				if (i < limit) { // found
					topFiles.add(i, newF);
					if (topFiles.size() > n) // more than limit, so discard the
												// last one. Maintain list at
												// size n
						topFiles.removeLast();
				}
			}
			return false;
		}

	}
}
