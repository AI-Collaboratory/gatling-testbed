package bd.ciber.testbed;

import java.util.Iterator;

import org.junit.Test;

public class CiberIndexTest {

	@Test
	public void testGet5000() {
		CiberQueryBuilder cqb = new CiberQueryBuilder().limit(5000).minBytes(100).maxBytes((int) 20e6).excludeExtensions("SHX", "SHP");
		int count = 0; 
		for(String path : cqb) {
			count++;
		}
		System.out.println("Got "+count);
	}
	
	@Test
	public void testGetPublicURLs() {
		CiberQueryBuilder cqb = new CiberQueryBuilder().makePublicURLs().limit(100).minBytes(100).maxBytes((int) 20e6).excludeExtensions("SHX", "SHP");
		for(String url : cqb) {
			System.out.println(url);
		}
	}
	
	@Test
	public void testGetUnlimited500() {
		CiberQueryBuilder cqb = new CiberQueryBuilder().minBytes(100).maxBytes((int) 20e6).excludeExtensions("SHX", "SHP");
		Iterator<String> iter = cqb.iterator();
		for(int i = 0; i <= 500; i++) {
			if(iter.hasNext()) iter.next();
		}
	}

	@Test
	public void testGetUniqueFormats() {
		Iterator<String> formats = new CiberQueryBuilder().limit(400).getUniqueFormats();
		while(formats.hasNext()) System.out.println(formats.next());
		
	}

}
