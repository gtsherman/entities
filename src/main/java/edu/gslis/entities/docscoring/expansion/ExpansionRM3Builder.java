package edu.gslis.entities.docscoring.expansion;

import java.util.Map;

import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.queries.GQuery;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class ExpansionRM3Builder {
	
	private ExpansionRM1Builder rm1;
	private GQuery query;
	
	public ExpansionRM3Builder(GQuery query, ExpansionRM1Builder rm1) {
		setRM1Builder(rm1);
		setQuery(query);
	}
	
	public void setRM1Builder(ExpansionRM1Builder rm1) {
		this.rm1 = rm1;
	}
	
	public void setQuery(GQuery query) {
		this.query = query;
		this.query.getFeatureVector().normalize(); // very important
	}
	
	public FeatureVector buildRelevanceModel(Map<String, Double> parameters) {
		return buildRelevanceModel(null, parameters);
	}

	public FeatureVector buildRelevanceModel(Stopper stopper, Map<String, Double> parameters) {
		FeatureVector rmVector = rm1.buildRelevanceModel(stopper, parameters);
		rmVector.normalize(); // very important
		return FeatureVector.interpolate(query.getFeatureVector(), rmVector, parameters.get(RMRunner.ORIG_QUERY_WEIGHT));
	}

}
