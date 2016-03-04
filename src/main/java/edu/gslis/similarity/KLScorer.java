package edu.gslis.similarity;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.textrepresentation.FeatureVector;

public class KLScorer {
	
	private static Logger logger = LoggerFactory.getLogger(KLScorer.class);
	
	public static double scoreKL(FeatureVector doc, FeatureVector otherdoc) {
		double score = 0;
		
		Iterator<String> termIt = doc.iterator();
		while (termIt.hasNext()) {
			String term = termIt.next();
			
			logger.debug("Term weight 1: " + doc.getFeatureWeight(term) + " Doc length 1: " + doc.getLength());
			logger.debug("Term weight 2: " + otherdoc.getFeatureWeight(term) + " Doc length 2: " + otherdoc.getLength());
			
			double pwd = doc.getFeatureWeight(term) / doc.getLength();
			double pwc = (otherdoc.getFeatureWeight(term) + 1) / (otherdoc.getLength() + doc.getFeatureCount());
			
			if (pwd == 0) {
				continue;
			} else {
				score += pwd * Math.log(pwd / pwc);
			}
		}
		
		return score;
	}

}
