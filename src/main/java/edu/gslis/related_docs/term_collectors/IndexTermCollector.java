package edu.gslis.related_docs.term_collectors;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class IndexTermCollector implements TermCollector {

	private RelatedDocs clusters;
	private IndexWrapper index;
	
	public IndexTermCollector(RelatedDocs clusters, IndexWrapper index) {
		this.clusters = clusters;
		this.index = index;
	}

	@Override
	public FeatureVector getTerms(SearchHit doc, Stopper stopper) {
		FeatureVector terms = new FeatureVector(null);
		if (clusters.getDocsRelatedTo(doc) != null) {
			System.err.println(clusters.getDocsRelatedTo(doc).size() + " docs related to " + doc.getDocno());
			for (String docno : clusters.getDocsRelatedTo(doc).keySet()) {
				SearchHit expansionHit = new IndexBackedSearchHit(index);
				expansionHit.setDocno(docno);
				for (String term : expansionHit.getFeatureVector().getFeatures()) {
					if (stopper != null && stopper.isStopWord(term)) {
						continue;
					}
					terms.addTerm(term, doc.getFeatureVector().getFeatureWeight(term));
				}
			}
		}
		return terms;
	}

}
