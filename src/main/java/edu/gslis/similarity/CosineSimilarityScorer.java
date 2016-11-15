package edu.gslis.similarity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.textrepresentation.FeatureVector;

public class CosineSimilarityScorer {
	
	private FeatureVector doc;
	private FeatureVector otherdoc;
	
	public CosineSimilarityScorer(FeatureVector doc, FeatureVector otherdoc) {
		setDocs(doc, otherdoc);
	}
	
	public void setDocs(FeatureVector doc, FeatureVector otherdoc) {
		this.doc = doc;
		this.otherdoc = otherdoc;
	}
	
	public FeatureVector[] getDocs() {
		FeatureVector[] docs = {this.doc, this.otherdoc};
		return docs;
	}
	
	public double score() {
		/*double num = 0.000001;
		double denomX = 0.000001;
		double denomY = 0.000001;
		*/
		double num = 0.0;
		double denomX = 0.0;
		double denomY = 0.0;
		
		Set<String> vocab = new HashSet<String>();
		vocab.addAll(doc.getFeatures());
		vocab.addAll(otherdoc.getFeatures());

		Iterator<String> termIt = vocab.iterator();
		String term;
		while (termIt.hasNext()) {
			term = termIt.next();
			
			double docWeight = doc.getFeatureWeight(term);
			double otherdocWeight = otherdoc.getFeatureWeight(term); 
			
			num += docWeight * otherdocWeight;
			denomX += Math.pow(docWeight, 2);
			denomY += Math.pow(otherdocWeight, 2);
		}
		double denom = Math.sqrt(denomX)*Math.sqrt(denomY);
		if (denom == 0) denom = 1;  // should be unnecessary...
		return num/denom;
	}
}
