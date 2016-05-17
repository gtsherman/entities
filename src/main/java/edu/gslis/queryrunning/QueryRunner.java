package edu.gslis.queryrunning;

import java.util.Iterator;

import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHits;

public interface QueryRunner {

	public void setQueries(GQueries queries);
	
	/**
	 * Execute the queries.
	 * @param numResults The maximum number of documents to return per query
	 */
	public void run(int numResults);
	
	public SearchHits getQueryResults(String queryTitle);
	
	public SearchHitsBatch getBatchResults();
	
	public Iterator<String> iterator();
	
	public void setParameter(String parameter, Double value);
	
}
