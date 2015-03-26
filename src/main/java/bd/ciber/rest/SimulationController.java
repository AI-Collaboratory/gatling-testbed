package bd.ciber.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Produces a landing page for each simulation.
 * 
 * @author greg
 *
 */
@RestController
@RequestMapping("/sim/")
public class SimulationController {
	private static final Logger LOG = LoggerFactory
			.getLogger(SimulationController.class);
	
	Map<String, Map> simulationsJSON = null;
	
	@PostConstruct
	public void init() throws JsonParseException, JsonMappingException, IOException {
		// load simulations.json file for templates
		InputStream is = SimulationController.class.getResourceAsStream("/simulations.json");
		simulationsJSON = new ObjectMapper().readValue(is, HashMap.class);		
	}
	
	@RequestMapping(value="/{sim}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable String sim) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(simulationsJSON.get(sim));
		modelAndView.addObject("id", sim);
        modelAndView.setViewName("sim");
        return modelAndView;
	}
	
	@RequestMapping(value="/", method = RequestMethod.GET, headers = "Accept=application/json")
	public Map<String, Map> getSimulationsList() {
		return simulationsJSON;
	}
}
