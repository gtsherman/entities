package edu.gslis.evaluation.running.runners.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;

/**
 * Storage and manipulation of parameterized search results.
 * Parameter meaning is defined by order. Two objects with differently
 * ordered parameters are considered to have different parameters.
 * 
 * @author garrick
 *
 */
public class ParameterizedResults {
	
	private Map<ParameterizedResultsKey, SearchHits> presults;
	private Map<ParameterizedResultsKey, Double> pscores;
	
	public ParameterizedResults() {
		presults = new HashMap<ParameterizedResultsKey, SearchHits>();
		pscores = new HashMap<ParameterizedResultsKey, Double>();
	}
	
	public void addResults(SearchHits results, GQuery query, double... params) {
		ParameterizedResultsKey key = new ParameterizedResultsKey(query, params);
		presults.put(key, results);
	}
	
	public void setScore(double score, GQuery query, double... params) {
		ParameterizedResultsKey key = new ParameterizedResultsKey(query, params);
		pscores.put(key, score);
	}
	
	public boolean resultsExist(GQuery query, double... params) {
		return presults.containsKey(new ParameterizedResultsKey(query, params));
	}
	
	public boolean scoreExists(GQuery query, double... params) {
		return pscores.containsKey(new ParameterizedResultsKey(query, params));
	}
	
	public SearchHits getResults(GQuery query, double... params) {
		return presults.get(new ParameterizedResultsKey(query, params));
	}
	
	public double getScore(GQuery query, double... params) {
		return pscores.get(new ParameterizedResultsKey(query, params));
	}


	class ParameterizedResultsKey {
		
		private GQuery query;
		private double[] params;
		
		public ParameterizedResultsKey(GQuery query, double[] params) {
			this.query = query;
			this.params = params;
		}
		
		public GQuery getQuery() {
			return query;
		}
		
		public double[] getParams() {
			return params;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof ParameterizedResultsKey) {
				ParameterizedResultsKey k = (ParameterizedResultsKey)obj;
				return query.getTitle().equals(k.getQuery().getTitle()) && Arrays.equals(params, k.getParams());
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			String s = new String();
			s += query.getTitle();
			for (double p : params) {
				s += p;
			}
			return s.hashCode();
		}
		
	}

}
