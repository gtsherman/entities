package edu.gslis.entities.docscoring;

import edu.gslis.queries.GQuery;

public interface QueryScorer {

	public double scoreQuery(GQuery query);
	
}
