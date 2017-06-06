package bd.ciber.testbed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.mongodb.util.JSON;

import static bd.ciber.testbed.MongoKeys.*;

public class SimulationLogParserTest {

	@Test
	public void testParseErrors() throws IOException {
		File testLogFile = new File("src/test/resources/alloysimulation-1425400194812/simulation.log");
		Map<String, Object> result = SimulationLogParser.parse(testLogFile, false);
		List<Map<String, Object>> errors = (List<Map<String, Object>>)result.get(MongoKeys.ERRORS); 
		assertEquals("Number of errors in log", 2, errors.size());
		Object dt = errors.get(0).get(DATETIME);
		assertTrue("Must be a date/time", Date.class.isInstance(dt));
		Date date = (Date)dt;
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);
		assertTrue("Must be a date in 2015",  cal.get(Calendar.YEAR) == 2015);
		
		assertTrue("request_login".equals(errors.get(0).get(ERROR_REQUEST_NAME)));
		assertTrue("request_container".equals(errors.get(1).get(ERROR_REQUEST_NAME)));
		
		assertTrue(((String)errors.get(0).get(ERROR_MESSAGE)).startsWith("java.net.ConnectException:"));
		assertTrue(((String)errors.get(1).get(ERROR_MESSAGE)).startsWith("java.net.ConnectException:"));
	}

	@Test
	public void testParseMetrics() throws IOException {
		File testLogFile = new File("src/test/resources/simulation1.log");
		Map<String, Object> result = SimulationLogParser.parse(testLogFile, false);
		
		Map<String, Long> metrics = (Map<String, Long>)result.get(MongoKeys.BDMETRICS);
		assertTrue(3 == metrics.size());
		assertTrue((metrics.get(BDMETRICS_MAX) >= metrics.get(BDMETRICS_MEAN)));
		assertTrue((metrics.get(BDMETRICS_MEAN) >= metrics.get(BDMETRICS_MIN)));
	}
	
	@Test
	public void testParseMetrics2() throws IOException {
		File testLogFile = new File("src/test/resources/simulation2.log");
		Map<String, Object> result = SimulationLogParser.parse(testLogFile, false);
		
		Map<String, Long> metrics = (Map<String, Long>)result.get(MongoKeys.BDMETRICS);
		assertTrue(3 == metrics.size());
		assertTrue((metrics.get(BDMETRICS_MAX) >= metrics.get(BDMETRICS_MEAN)));
		assertTrue((metrics.get(BDMETRICS_MEAN) >= metrics.get(BDMETRICS_MIN)));
	}
	
}
