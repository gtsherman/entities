package edu.gslis.queryrunning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHits;

public abstract class GenericRunner implements QueryRunner {

	protected GQueries queries;
	protected SearchHitsBatch batchResults;
	
	protected Map<String, Double> params;
	
	public void setQueries(GQueries queries) {
		this.queries = queries;
	}

	public SearchHits getQueryResults(String queryTitle) {
		if (batchResults != null)
			return batchResults.getSearchHits(queryTitle);
		return null;
	}

	public SearchHitsBatch getBatchResults() {
		return batchResults;
	}

	public Iterator<String> iterator() {
		return batchResults.queryIterator();
	}

	public void setParameter(String parameter, Double value) {
		if (params == null)
			params = new HashMap<String, Double>();

		params.put(parameter, value);
	}

}
