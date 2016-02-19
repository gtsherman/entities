package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.searchhits.SearchHit;

public class BackgroundProbability implements CategoryProbability {
	private String thisClass = "[BackgroundProbability] "; 

	private CollectionStats cs;
	
	public BackgroundProbability() {
	}
	
	public void setDocument(SearchHit document) {
		System.err.println(thisClass+"Document: "+document.getDocno());
	}
	
	public void setCollectionStats(CollectionStats cs) {
		this.cs = cs;
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();
		
		Set<String> termSet = new HashSet<String>(terms);

		System.err.println(thisClass+"Using index.");

		for (String term : termSet) {
			double collectionScore = 0.0;
			if (cs != null) {
				collectionScore = (1.0 + cs.termCount(term)) / cs.getTokCount();
			}
			System.err.println(thisClass+term+": "+collectionScore);
			double score = collectionScore;
			termProbs.put(term, score);
		}
	
		System.err.println(thisClass+"Term probabilities:");
		for (String term : termProbs.keySet()) {
			System.err.println("\t"+term+" "+termProbs.get(term));
		}
			
		return termProbs;
	}
}
