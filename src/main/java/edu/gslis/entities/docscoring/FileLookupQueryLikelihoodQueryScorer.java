package edu.gslis.entities.docscoring;

import java.io.File;

import edu.gslis.queries.GQuery;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.searchhits.SearchHit;

public class FileLookupQueryLikelihoodQueryScorer extends QueryLikelihoodQueryScorer {

	public FileLookupQueryLikelihoodQueryScorer(FileLookupDocScorer termScorer) {
		super(termScorer);
	}
	
	@Override
	public double scoreQuery(GQuery query, SearchHit document) {
		// Store the original docno
		String docno = document.getDocno();
		
		// Set the docno to reflect the query
		document.setDocno(query.getTitle() + File.separator + docno);
		
		// Actually score it (i.e. look up the scores and combine)
		double score = super.scoreQuery(query, document);
		
		// Reset the docno to cut down on side effects
		document.setDocno(docno);
		
		// Finally, return the score
		return score;
	}

}
