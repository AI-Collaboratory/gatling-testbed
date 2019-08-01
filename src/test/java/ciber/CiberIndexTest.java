package umd.ciber.ciber_sampling;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CiberIndexTest {


	public void testGet5000() {
		CiberQueryBuilder cqb = new CiberQueryBuilder().minBytes(100).maxBytes((int) 20e6).excludeExtensions("SHX", "SHP");
		int count = 0;
		for(String path : cqb) {
			count++;
			if(count >= 20000) break;
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
		Iterator<String> formats = new CiberQueryBuilder().getUniqueFormats();
    int c = 0;
		while(formats.hasNext()) {
      System.out.println(formats.next());
      c++;
    }
    System.out.println("found formats: "+c);
	}

}
