package edu.gslis.related_docs.term_collectors;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class MultiSourceTermCollector implements TermCollector {
	
	private TermCollector[] collectors;

	public MultiSourceTermCollector(TermCollector... collectors) {
		this.collectors = collectors;
	}

	@Override
	public FeatureVector getTerms(SearchHit doc, Stopper stopper) {
		FeatureVector terms = new FeatureVector(null);
		for (TermCollector collector : collectors) {
			FeatureVector collectorTerms = collector.getTerms(doc, stopper);
			for (String term : collectorTerms) {
				terms.addTerm(term, collectorTerms.getFeatureWeight(term));
			}
		}
		return terms;
	}

}
