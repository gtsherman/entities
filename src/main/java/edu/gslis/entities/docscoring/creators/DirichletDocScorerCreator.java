package edu.gslis.entities.docscoring.creators;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.entities.docscoring.DirichletDocScorer;
import edu.gslis.entities.docscoring.StoredDocScorer;
import edu.gslis.searchhits.SearchHit;

public class DirichletDocScorerCreator extends DocScorerCreator {
	
	private CollectionStats collectionStats;
	private double mu = -1;
	
	public DirichletDocScorerCreator(CollectionStats collectionStats) {
		this.collectionStats = collectionStats;
	}
	
	public DirichletDocScorerCreator(double mu, CollectionStats collectionStats) {
		this(collectionStats);
		this.mu = mu;
	}
	
	public CollectionStats getCollectionStats() {
		return collectionStats;
	}
	
	public double getMu() {
		return mu;
	}
	
	@Override
	protected void createIfNecessary(SearchHit doc) {
		String docKey = docKey(doc);
		if (!storedScorers.containsKey(docKey)) {
			DirichletDocScorer docScorer = new DirichletDocScorer(doc, collectionStats);
			if (mu > -1) {
				docScorer.setMu(mu);
			}
			storedScorers.put(docKey, new StoredDocScorer(docScorer));
		}
	}
	
	@Override
	protected String docKey(SearchHit doc) {
		return doc.getDocno() + mu;
	}

}
