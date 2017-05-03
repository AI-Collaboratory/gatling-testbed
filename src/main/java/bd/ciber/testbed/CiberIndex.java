package bd.ciber.testbed;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;

public class CiberIndex {
	private static final Logger LOG = LoggerFactory.getLogger(CiberIndex.class);

	MongoClient mongoClient;
	
	BasicDBObject fullpathSpec = new BasicDBObject(CiberIndexKeys.F_FULLPATH.key(), 1);

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}
	
	/**
	 * Iterates through a sample of the CI-BER data. Will return a consistent
	 * sample whenever a same randomSeed is supplied.
	 * then 
	 * @param howMany limit the number of results or 0 for no limit
	 * @param randomSeed an offset for sampling at random, or null for new random sample.
	 * @param minSize minimum size of files in bytes
	 * @param maxSize maximum size of files in bytes
	 * @param extension the desired file extensions, if any (case insensitive)
	 * @return a Gatling-style iterator of mapped "fullpath"
	 */
	public Iterator<String> get(int howMany, Float randomSeed, Integer minSize, Integer maxSize, Boolean includeExtensions, String... extension) {
		DB ciberCatDB = mongoClient.getDB(CiberIndexKeys.DB.key());
		DBCollection coll = ciberCatDB.getCollection(CiberIndexKeys.FILES_COLL.key());
		
		QueryBuilder qb = QueryBuilder.start();
		if(extension != null && extension.length > 0) {
			for(int i = 0; i < extension.length; i++) {
				extension[i] = extension[i].toUpperCase();
			}
			if(includeExtensions) {
				qb.and(CiberIndexKeys.F_EXTENSION.key()).in(extension);
			} else {
				qb.and(CiberIndexKeys.F_EXTENSION.key()).notIn(extension);
			}
		}
		if(minSize != null) {
			qb.and(CiberIndexKeys.F_SIZE.key()).greaterThanEquals(minSize);
		}
		if(maxSize != null) {
			qb.and(CiberIndexKeys.F_SIZE.key()).lessThanEquals(maxSize);
		}
		if(randomSeed == null) {
			randomSeed = new Float(Math.random());
		}
		qb.and(CiberIndexKeys.F_RANDOM.key()).greaterThan(randomSeed.floatValue());
		DBObject query = qb.get();
		LOG.info("QUERY: {} ", query.toString());
		DBCursor cursor = coll.find(query, fullpathSpec);
		if(howMany > 0) {
			cursor = cursor.sort(new BasicDBObject(CiberIndexKeys.F_RANDOM.key(), 1));
			cursor = cursor.limit(howMany);
		}
		Iterator<String> result = new MongoPathIterator(cursor);
		return result;
	}
	
	public static class MongoPathIterator implements Iterator<String>, Closeable {
		DBCursor cursor;
		
		public MongoPathIterator(DBCursor cursor) {
			this.cursor = cursor;
		}
		
		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public String next() {
			DBObject obj = cursor.next();
			String fullpath = (String)obj.get(CiberIndexKeys.F_FULLPATH.key());
			return fullpath;
		}

		@Override
		public void close() throws IOException {
			this.cursor.close();
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		
	}
}
