package edu.gslis.entities.docscoring.creators;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.entities.docscoring.StoredDocScorer;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.searchhits.SearchHit;

public class ExpansionDocsDocScorerCreator extends DocScorerCreator {
	
	private IndexWrapper expansionIndex;
	private RelatedDocs clusters;
	private double mu = -1;
	
	public ExpansionDocsDocScorerCreator(IndexWrapper expansionIndex, RelatedDocs clusters) {
		this.expansionIndex = expansionIndex;
		this.clusters = clusters;
	}
	
	public ExpansionDocsDocScorerCreator(double mu, IndexWrapper expansionIndex, RelatedDocs clusters) {
		this(expansionIndex, clusters);
		this.mu = mu;
	}

	@Override
	protected void createIfNecessary(SearchHit doc) {
		if (!storedScorers.containsKey(doc.getDocno())) {
			ExpansionDocsDocScorer docScorer;
			if (mu > -1) {
				docScorer = new ExpansionDocsDocScorer(mu, doc, expansionIndex, clusters);
			} else {
				docScorer = new ExpansionDocsDocScorer(doc, expansionIndex, clusters);
			}
			storedScorers.put(doc.getDocno(), new StoredDocScorer(docScorer));
		}
	}

}
