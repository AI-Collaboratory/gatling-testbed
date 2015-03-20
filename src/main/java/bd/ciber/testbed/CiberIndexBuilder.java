package bd.ciber.testbed;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class CiberIndexBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(CiberIndexBuilder.class);
	
	private Random random = new XORShiftRandom();
	
	MongoClient mongoClient;

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}

	/**
	 * Create the MongoDB collection
	 * 
	 * @throws UnknownHostException
	 */
	@PostConstruct
	public void init() throws UnknownHostException {
		DB ciberCatDB = mongoClient.getDB(CiberIndexKeys.DB.key());
		Set<String> colls = ciberCatDB.getCollectionNames();
		if (!colls.contains(CiberIndexKeys.FILES_COLL.key())) {
			DBCollection files = ciberCatDB.createCollection(
					CiberIndexKeys.FILES_COLL.key(), null);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_DEPTH);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_SIZE);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_NAME);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_FOLDER);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_FULLPATH);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_EXTENSION,
					CiberIndexKeys.F_SIZE);
			addIndexWithRandomSampling(files, CiberIndexKeys.F_FOLDER,
					CiberIndexKeys.F_EXTENSION, CiberIndexKeys.F_SIZE);
		}
	}

	/**
	 * Adds a compound index for the given keys, adding random field for
	 * sampling
	 * 
	 * @param files
	 * @param fExtension
	 */
	private void addIndexWithRandomSampling(DBCollection files,
			CiberIndexKeys... keys) {
		BasicDBObject spec = new BasicDBObject();
		for (CiberIndexKeys key : keys) {
			spec.append(key.key(), 1);
		}
		spec.append(CiberIndexKeys.F_RANDOM.key(), 1);
		files.createIndex(spec);
	}

	public void rebuild(File inventoryFile) throws IOException {
		DB ciberCatDB = mongoClient.getDB(CiberIndexKeys.DB.key());
		DBCollection filesColl = ciberCatDB
				.getCollection(CiberIndexKeys.FILES_COLL.key());
		long lines = countFiles(inventoryFile);
		LineIterator it = FileUtils.lineIterator(inventoryFile, "UTF-8");
		BulkWriteOperation op = filesColl.initializeUnorderedBulkOperation();
		int ready = 0;
		long inserted = 0;
		long started = System.currentTimeMillis();
		try {
			while (it.hasNext()) {
				String line = it.nextLine();
				op.insert(parseLine(line));
				ready++;
				if (ready >= 1000) {
					op.execute(WriteConcern.ACKNOWLEDGED);
					inserted = inserted + ready;
					ready = 0;
					op = filesColl.initializeUnorderedBulkOperation();
					updateConsole(inserted, lines, started);
				}
			}
			if (ready > 0) {
				op.execute(WriteConcern.ACKNOWLEDGED);
			}
		} finally {
			LineIterator.closeQuietly(it);
		}
		if(inserted % 100000 == 0) updateConsole(inserted, lines, started);
	}

	private void updateConsole(long inserted, long lines, long started) {
		float done = (float) inserted / lines;
		long elapsed = System.currentTimeMillis() - started;
		float rate = inserted / elapsed;
		long millisRemaining = (long) ((lines - inserted) / rate);
		long estimateFinish = System.currentTimeMillis() + millisRemaining;
		float hoursRemaining = (float) millisRemaining / (1000f * 60f * 60f);
		System.out
				.print("\r                                                                          ");
		String fmt = "{0,number,percent} done, {1,number} hours remain, estimated finish {2,time,short}";
		String msg = MessageFormat.format(fmt, done, hoursRemaining,
				estimateFinish);
		System.out.print("\r");
		System.out.print(msg);
	}

	/**
	 * count lines in inventory
	 * 
	 * @param inventory
	 * @return number of lines (files)
	 * @throws IOException
	 */
	private static long countFiles(File inventory) throws IOException {
		long result = 0;
		LineIterator it = FileUtils.lineIterator(inventory, "UTF-8");
		try {
			while (it.hasNext()) {
				String line = it.nextLine();
				result++;
			}
		} finally {
			LineIterator.closeQuietly(it);
		}
		return result;
	}

	/**
	 * Parses inventory file lines. Inventory building command: 
	 * find . -type f -fprintf ~/inventory.txt "%f\t%h\t%d\t%s\n"
	 * Note: run command on current dir only (.)
	 * 
	 * @param line
	 * @return
	 */
	private BasicDBObject parseLine(String line) {
		BasicDBObject obj = new BasicDBObject();
		String[] fields = line.split("\\t");
		// TODO JSON-LD predicates/namespace
		// TODO add extension field (including blank ones)
		obj.append(CiberIndexKeys.F_NAME.key(), fields[0]);
		String folder = fields[1].substring(2);
		obj.append(CiberIndexKeys.F_FOLDER.key(), folder);
		StringBuilder sb = new StringBuilder(folder);
		sb.append('/').append(fields[0]);
		obj.append(CiberIndexKeys.F_FULLPATH.key(), sb.toString());
		obj.append(CiberIndexKeys.F_DEPTH.key(), fields[2]);
		obj.append(CiberIndexKeys.F_SIZE.key(), fields[3]);
		obj.append(CiberIndexKeys.F_RANDOM.key(), random.nextFloat());
		return obj;
	}

	private class XORShiftRandom extends Random {
		private static final long serialVersionUID = 1L;
		private long seed = System.nanoTime();

		public XORShiftRandom() {
		}

		protected int next(int nbits) {
			// N.B. Not thread-safe!
			long x = this.seed;
			x ^= (x << 21);
			x ^= (x >>> 35);
			x ^= (x << 4);
			this.seed = x;
			x &= ((1L << nbits) - 1);
			return (int) x;
		}
	}

	public static void main(String[] args) throws IOException {
		File inventoryFile = new File(args[1]);
		if (!inventoryFile.exists()) {
			LOG.error("Inventory file not found: {}", args[1]);
			return;
		}
		MongoClient c = new MongoClient();
		CiberIndexBuilder builder = new CiberIndexBuilder();
		builder.setMongoClient(c);
		builder.init();
		builder.rebuild(inventoryFile);
	}
}
