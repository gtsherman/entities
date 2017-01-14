package edu.gslis.entities.docscoring;

import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.searchhits.SearchHit;

public class CachedFileLookupDocScorer extends FileLookupDocScorer {
	
	private CachedDocScorer docScorer;

	public CachedFileLookupDocScorer(String basePath) {
		super(basePath);
		docScorer = new CachedDocScorer(new FileLookupDocScorer(basePath));
	}
	
	@Override
	public double scoreTerm(String term, SearchHit document) {
		return docScorer.scoreTerm(term, document);
	}

}
