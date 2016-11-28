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
	
	public IndexWrapper getIndex() {
		return expansionIndex;
	}
	
	public RelatedDocs getClusters() {
		return clusters;
	}
	
	public double getMu() {
		return mu;
	}

	@Override
	protected void createIfNecessary(SearchHit doc) {
		String docKey = docKey(doc);
		if (!storedScorers.containsKey(docKey)) {
			ExpansionDocsDocScorer docScorer;
			if (mu > -1) {
				docScorer = new ExpansionDocsDocScorer(mu, doc, expansionIndex, clusters);
			} else {
				docScorer = new ExpansionDocsDocScorer(doc, expansionIndex, clusters);
			}
			storedScorers.put(docKey, new StoredDocScorer(docScorer));
		}
	}
	
	@Override
	protected String docKey(SearchHit doc) {
		return doc.getDocno() + mu;
	}

}
