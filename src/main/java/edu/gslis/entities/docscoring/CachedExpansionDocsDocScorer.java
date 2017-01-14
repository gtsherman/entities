package edu.gslis.entities.docscoring;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.searchhits.SearchHit;

public class CachedExpansionDocsDocScorer extends ExpansionDocsDocScorer {
	
	private CachedDocScorer docScorer;

	public CachedExpansionDocsDocScorer(IndexWrapper expansionIndex, RelatedDocs clusters) {
		super(expansionIndex, clusters);
		createCachedDocScorer();
	}

	public CachedExpansionDocsDocScorer(double mu, IndexWrapper expansionIndex, RelatedDocs clusters) {
		super(mu, expansionIndex, clusters);
		createCachedDocScorer();
	}
	
	private void createCachedDocScorer() {
		docScorer = new CachedDocScorer(
				new ExpansionDocsDocScorer(getMu(), getExpansionIndex(), getClusters()));
	}
	
	@Override
	public double scoreTerm(String term, SearchHit doc) {
		return docScorer.scoreTerm(term, doc);
	}

}
