package edu.gslis.entities.docscoring.support;

import java.util.List;
import java.util.Map;

import edu.gslis.entities.readers.PrecomputedCategoryProbabilityReader;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

public class PrecomputedCategoryProbability implements CategoryProbability {

	private static String thisClass = "[PrecomputedCategoryProbability] ";

	private SearchHit document;
	private String query;
	
	private PrecomputedCategoryProbabilityReader reader;
	
	public PrecomputedCategoryProbability(String file) {
		reader = new PrecomputedCategoryProbabilityReader();
		reader.readFileAbsolute(file);
	}
	
	public PrecomputedCategoryProbability(String file, GQuery query) {
		this(file, query.getTitle());
	}
	
	public PrecomputedCategoryProbability(String file, String query) {
		this(file);
		setQuery(query);
	}
	
	public void setQuery(GQuery query) {
		this.query = query.getTitle();
	}
	
	/**
	 * Tells us which query to read probabilities for.
	 * @param query	The query title (NOT the text of the query)
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	public void setDocument(SearchHit document) {
		this.document = document;
	}
	
	public Map<String, Double> getProbability(List<String> terms) {
		if (query == null) {
			System.err.println(thisClass+"Must specify query before getting probabilities.");
			System.exit(-1);
		}
		Map<Integer, Map<String, Double>> probs = getQueryProbabilities();
		return probs.get(document.getDocID());
	}
	
	protected Map<Integer, Map<String, Double>> getQueryProbabilities() {
		return reader.getProbabilities().get(query);
	}

}
