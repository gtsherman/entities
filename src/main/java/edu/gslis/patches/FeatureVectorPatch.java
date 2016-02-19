package edu.gslis.patches;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class FeatureVectorPatch extends FeatureVector {

	public FeatureVectorPatch(String text, Stopper stopper) {
		super(text, stopper);
	}
	
	public FeatureVectorPatch(Stopper stopper) {
		super(stopper);
	}

	@Override
	public void addTerm(String term, double weight) {
		for (int i = 0; i < weight; i++) {
			addTerm(term);
		}
	}
}
