package bd.ciber.testbed;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;

public class CiberIndex {

	MongoClient mongoClient;
	
	BasicDBObject fullpathSpec = new BasicDBObject(CiberIndexKeys.F_FULLPATH.key(), 1);

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}
	
	public Iterator<Map<String, String>> get(int howMany, boolean randomlySample, Integer minSize, Integer maxSize, String... extension) {
		DB ciberCatDB = mongoClient.getDB(CiberIndexKeys.DB.key());
		DBCollection coll = ciberCatDB.getCollection(CiberIndexKeys.FILES_COLL.key());
		
		QueryBuilder qb = QueryBuilder.start();
		if(extension != null && extension.length > 0) {
			qb.and(CiberIndexKeys.F_EXTENSION.key()).in(extension);
		}
		if(minSize != null) {
			qb.and(CiberIndexKeys.F_SIZE.key()).greaterThanEquals(minSize);
		}
		if(maxSize != null) {
			qb.and(CiberIndexKeys.F_SIZE.key()).lessThanEquals(maxSize);
		}
		if(randomlySample) {
			qb.and(CiberIndexKeys.F_RANDOM.key()).greaterThan(Math.random());
		}
		DBCursor cursor = coll.find(qb.get(), fullpathSpec);
		if(randomlySample) {
			cursor.sort(new BasicDBObject(CiberIndexKeys.F_RANDOM.key(), 1));
		}
		cursor.limit(howMany);
		Iterator<Map<String, String>> result = new MongoIterator(cursor);
		return result;
	}
	
	public static class MongoIterator implements Iterator<Map<String, String>> {
		DBCursor cursor;
		
		public MongoIterator(DBCursor cursor) {
			this.cursor = cursor;
		}
		
		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public Map<String, String> next() {
			DBObject obj = cursor.next();
			String fullpath = (String)obj.get(CiberIndexKeys.F_FULLPATH.key());
			Map<String, String> result = Collections.singletonMap("fullpath", fullpath);
			return result;
		}
		
	}
}
