package edu.gslis.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.entities.utils.Configuration;
import edu.gslis.entities.utils.SimpleConfiguration;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class ProduceReusableEntityProbabilities {

	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		DocumentEntityReader de = new DocumentEntityReader();
		de.readFileAbsolute(config.get("document-entities-file"));
		
		String outputDir = config.get("output-dir");
		
		for (String document : de.getDocuments()) {
			List<String> entities = de.getEntities(document);
			Set<String> entitySet = new HashSet<String>(entities);

			Map<String, FeatureVector> entityVectors = new HashMap<String, FeatureVector>();
			FeatureVector combinedEntityVector = new FeatureVector(stopper);
			for (String entity : entitySet) {
				if (!entityVectors.containsKey(entity)) {
					entityVectors.put(entity, wikiIndex.getDocVector(entity, stopper));
				}
				FeatureVector entityVector = entityVectors.get(entity);
				Iterator<String> vectorIt = entityVector.iterator();
				while(vectorIt.hasNext()) {
					String term = vectorIt.next();
					combinedEntityVector.addTerm(term, entityVector.getFeatureWeight(term));
				}
			}
			
			try {
				PrintWriter out = new PrintWriter(new File(outputDir+"/"+document));
				for (String term : combinedEntityVector.getFeatures()) {
					out.println(term+" "+combinedEntityVector.getFeatureWeight(term));
				}
				out.close();
			} catch (FileNotFoundException e) {
				System.err.println("Cannot open file: "+outputDir+"/"+document);
			}
		}
	}

}
