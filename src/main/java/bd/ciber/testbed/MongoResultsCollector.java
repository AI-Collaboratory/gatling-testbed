package bd.ciber.testbed;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static bd.ciber.testbed.MongoKeys.*;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

@Component
public class MongoResultsCollector {
	private static final Logger LOG = LoggerFactory
			.getLogger(MongoResultsCollector.class);
	
	@Value("${resultsFolder}")
	private String resultsFolder;
	
	@PostConstruct
	public void init() throws UnknownHostException {
		// Create mongo collections
		MongoClient mongoClient = new MongoClient();
		DB testbedDB = mongoClient.getDB(DBNAME);
		Set<String> colls = testbedDB.getCollectionNames();
		if (!colls.contains(SIMULATION_RESULTS_COLL)) {
			DBCollection res = testbedDB.createCollection(SIMULATION_RESULTS_COLL, null);
			res.createIndex(new BasicDBObject(DATETIME, 1));
		}
	}
	
	public static void main(String[] args) throws UnknownHostException {
		File folder = new File(args[0]);
		if(!folder.exists() || !folder.isDirectory()) {
			System.err.println(args[0]+" not a valid Gatling results folder.");
			System.exit(1);
		}
		MongoResultsCollector instance = new MongoResultsCollector();
		instance.resultsFolder = folder.getAbsolutePath();
		instance.init();
		instance.collectAll();
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
		LOG.info("Collecting {} simulation results to MongoDB", foldersToExamine);
		File[] resFolders = getLatestResultsFolders(foldersToExamine);
		for(File resFolder : resFolders) {
			String fldNm = resFolder.getName();
			String simName = fldNm.substring(0, fldNm.indexOf('-')).toLowerCase();
			collectSimulationResult(resFolder, simName);
		}
	}

	/**
	 * Collect the results of all simulations into MongoDB. Will
	 * perform upsert to update existing documents.
	 * 
	 */
	public void collectAll() {
		File dir = new File(this.resultsFolder);
		File[] resFolders = dir.listFiles();
		int count = 0;
		for(File resFolder : resFolders) {
			String fldNm = resFolder.getName();
			String simName = fldNm.substring(0, fldNm.indexOf('-')).toLowerCase();
			collectSimulationResult(resFolder, simName);
			count++;
			if(count % 100 == 0) {
				System.out.println("Finished results folders: "+count);
			}
		}
	}
	
	private void collectSimulationResult(File resFolder, String simName) {
		try {
			MongoClient mongoClient = new MongoClient();
			DB testbedDB = mongoClient.getDB(DBNAME);
			DBCollection stats = testbedDB.getCollection(SIMULATION_RESULTS_COLL);
			
			// check that results folder is not already collected
			DBObject simRef = new BasicDBObject(GATLING_RESULTS_FOLDER, resFolder.getName());
			
			// add js/global_stats.json to document
			File globalStatsFile = new File(resFolder,
					"js/global_stats.json");
			if(!globalStatsFile.exists()) return; // skip incomplete folders
			String statsStr = FileUtils.readFileToString(globalStatsFile);
			DBObject statsObject = (DBObject) JSON.parse(statsStr);
			
			Date modified = new Date(resFolder.lastModified());
			statsObject.put(DATETIME, modified);
			statsObject.put(SIMULATION_NAME, simName);
			statsObject.put(GATLING_RESULTS_FOLDER, resFolder.getName());
			
			// add errors (KOs) from simulation.log
			File simulationLog = new File(resFolder, "simulation.log");
			try {
				Map<String, Object> parsedData = SimulationLogParser.parse(simulationLog);
				statsObject.putAll(parsedData);
			} catch(IOException e) {
				LOG.error("Unexpected IO Errors reading simulation log", e);
			}
			stats.update(simRef, statsObject, true, false);
		} catch (UnknownHostException e) {
			LOG.error("Unexpected", e);
			throw new Error(e);
		} catch (IOException e) {
			LOG.error("Unexpected", e);
			throw new Error(e);
		}
	}	
	
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
