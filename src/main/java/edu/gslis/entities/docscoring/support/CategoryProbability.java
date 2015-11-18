package edu.gslis.entities.docscoring.support;

import java.util.List;
import java.util.Map;

public interface CategoryProbability {
	
	public Map<String, Double> getProbability(List<String> terms);

}
