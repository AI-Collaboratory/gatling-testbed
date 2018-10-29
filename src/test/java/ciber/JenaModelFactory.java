package ciber;

import java.io.IOException;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class JenaModelFactory {
	private static final Logger log = LoggerFactory.getLogger(JenaModelFactory.class);
	
	public Model loadModelFile(String path) {
		if(path.endsWith(".hdt") || path.endsWith(".HDT")) {
			// Load HDT file using the hdt-java library
			try {
				HDT hdt = HDTManager.mapIndexedHDT("path/to/file.hdt", null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			// Create Jena Model on top of HDT.
			//HDTGraph graph = new HDTGraph(hdt);
			//Model model = ModelFactory.createModelForGraph(graph);
		}
		return null;
	}
}
