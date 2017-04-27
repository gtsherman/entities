package edu.gslis.entities.docscoring;

import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHit;

public class DatabaseLookupDocScorer implements DocScorer {
	
	private DocScorer mainScorer;
	private DocScorer backupScorer;

	public DatabaseLookupDocScorer(DocScorer mainScorer, DocScorer backupScorer) {
		this.mainScorer = mainScorer;
		this.backupScorer = backupScorer;
	}

	@Override
	public double scoreTerm(String term, SearchHit doc) {
		double score = mainScorer.scoreTerm(term, doc);
		if (score == 0.0) {
			score = backupScorer.scoreTerm(term, doc);
		}
		return score;
	}

}
