package edu.gslis.entities.docscoring.expansion;

import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class StoredRM1Builder implements RM1Builder {
	
	private FeatureVector rm1;

	public StoredRM1Builder(FeatureVector rm1) {
		this.rm1 = rm1;
	}

	@Override
	public FeatureVector buildRelevanceModel(GQuery query, SearchHits results) {
		return buildRelevanceModel(query, results);
	}

	@Override
	public FeatureVector buildRelevanceModel(GQuery query, SearchHits results, Stopper stopper) {
		return rm1;
	}

}
