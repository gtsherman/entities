package edu.gslis.entities.docscoring;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.searchhits.SearchHit;

public class CachedDirichletDocScorer extends DirichletDocScorer {
	
	private CachedDocScorer docScorer;

	public CachedDirichletDocScorer(CollectionStats collectionStats) {
		super(collectionStats);
		createCachedDocScorer();
	}

	public CachedDirichletDocScorer(double mu, CollectionStats collectionStats) {
		super(mu, collectionStats);
		createCachedDocScorer();
	}

	public CachedDirichletDocScorer(double mu, double epsilon, CollectionStats collectionStats) {
		super(mu, epsilon, collectionStats);
		createCachedDocScorer();
	}
	
	private void createCachedDocScorer() {
		docScorer = new CachedDocScorer(
				new DirichletDocScorer(getMu(), epsilon, getCollectionStats()));
	}
	
	@Override
	public double scoreTerm(String term, SearchHit doc) {
		return docScorer.scoreTerm(term, doc);
	}

}
