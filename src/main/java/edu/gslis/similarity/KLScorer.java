package edu.gslis.similarity;

import java.util.Iterator;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.textrepresentation.FeatureVector;

public class KLScorer {
	
	public static double scoreKL(FeatureVector doc, FeatureVector otherdoc) {
		double score = 0;
		
		Iterator<String> termIt = doc.iterator();
		while (termIt.hasNext()) {
			String term = termIt.next();
			
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
	
	public static double scoreKL(FeatureVector doc, IndexWrapper index) {
		double score = 0;
		
		Iterator<String> termIt = doc.iterator();
		while (termIt.hasNext()) {
			String term = termIt.next();
			
			double pwd = doc.getFeatureWeight(term) / doc.getLength();
			double pwc = index.termFreq(term) / index.termCount();
			
			if (pwc == 0 || pwd == 0) {
				continue;
			} else {
				score += pwd * Math.log(pwd / pwc);
			}
		}
		
		return score;
	}

}
