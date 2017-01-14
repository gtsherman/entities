package edu.gslis.readers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.sources.FileDataSource;

public class RMPrereader {
	
	public static final String WIKI = "wiki";
	public static final String SELF = "self";

	private Map<String, Map<String, FeatureVector>> rms;
	
	public RMPrereader(String enclosingDir) {
		rms = new HashMap<String, Map<String, FeatureVector>>();
		File dir = new File(enclosingDir);
		for (File doc : dir.listFiles()) {
			System.err.println(doc.getName());
			rms.put(doc.getName(), new HashMap<String, FeatureVector>());
			for (File rm : doc.listFiles()) {
				FileDataSource ds = new FileDataSource(rm);
				RelevanceModelDataInterpreter rmReader = new RelevanceModelDataInterpreter();
				if (rm.getName().equalsIgnoreCase(WIKI)) {
					rms.get(doc.getName()).put(WIKI, rmReader.build(ds));
				} else if (rm.getName().equalsIgnoreCase(SELF)) {
					rms.get(doc.getName()).put(SELF, rmReader.build(ds));
				}
			}
		}
	}
	
	public FeatureVector getRM(String docno, String model) {
		if (!rms.containsKey(docno)) {
			return null;
		}
		if (!rms.get(docno).containsKey(model)) {
			System.err.println("You probably want to specify one of "+WIKI+" or "+SELF);
			return null;
		}
		return rms.get(docno).get(model);
	}
	
}
