package edu.gslis.docscoring.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrefetchedCollectionStats extends IndexBackedCollectionStats {

	private Map<String, Double> featureScores;
	
	public PrefetchedCollectionStats(String pathToIndex, Set<String> features) {
		setStatSource(pathToIndex);
		precompute(features);
	}
	
	private void precompute(Set<String> features) {
		this.featureScores = new HashMap<String, Double>();
		for (String feature : features) {
			double score = (1.0 + termCount(feature)) / getTokCount();
			featureScores.put(feature, score);
		}
	}
	
	public double collectionScore(String feature) {
		return featureScores.containsKey(feature) ? featureScores.get(feature) : 0.0;
	}
	
}
