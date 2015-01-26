package bd.ciber.testbed;

import static org.junit.Assert.fail;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/service-context.xml")
public class SimulationTest {
	
	//@Resource
	GatlingSimulationRunner runner;

	@Test
	public void testPolyglot() {
		//runner.run("bd.ciber.gatling.PolyglotSimulation");
	}

}
