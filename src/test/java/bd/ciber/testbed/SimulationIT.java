package bd.ciber.testbed;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/service-context.xml")
public class SimulationIT {
		
	@Autowired
	GatlingSimulationRunner runner = new GatlingSimulationRunner();
	
	@Autowired
	MongoResultsCollector mongoResultsCollector;
	
	@Before
	public void initMocks(){
		Mockito.reset(mongoResultsCollector);
	}
	
	@Test
	public void testDocuments2PDFSimulation() throws ClassNotFoundException {
		runner.run("bd.ciber.gatling.Documents2PDFSimulation");
	}

}
