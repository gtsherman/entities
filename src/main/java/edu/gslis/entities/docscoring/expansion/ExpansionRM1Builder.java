package edu.gslis.entities.docscoring.expansion;

import java.util.Map;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public interface ExpansionRM1Builder {

	/**
	 * Build the relevance model without stopping.
	 * @param params The required parameters.
	 * @return A FeatureVector representing the RM1.
	 */
	public FeatureVector buildRelevanceModel(Map<String, Double> params);
	
	/**
	 * Build the relevance model with stopping.
	 * @param stopper The stopper to use.
	 * @param params The required parameters.
	 * @return A FeatureVector representing the RM1.
	 */
	public FeatureVector buildRelevanceModel(Stopper stopper, Map<String, Double> params);
	
	/**
	 * Set the query with its initial retrieval.
	 * @param query The query we want to expand.
	 * @param hits The initial retrieval for this query.
	 */
	public void setQuery(GQuery query, SearchHits hits);
	
	public void setFeedbackTerms(int feedbackTerms);

	public void setFeedbackDocs(int feedbackDocs);
	
}
