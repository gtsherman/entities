package edu.gslis.entities.docscoring.support;

import java.util.List;
import java.util.Map;

import edu.gslis.searchhits.SearchHit;

public interface EntityProbability {
	
	public void setDocument(SearchHit document);
	
	public Map<String, Double> getProbability(List<String> terms);

}
