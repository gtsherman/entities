package edu.gslis.related_docs.term_collectors;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public interface TermCollector {
	
	public FeatureVector getTerms(SearchHit doc, Stopper stopper);

}
