package edu.gslis.related_docs;

import java.util.HashMap;
import java.util.Map;

public class RelatedDocs {

	protected Map<String, Map<String, Double>> relatedDocScores = new HashMap<String, Map<String, Double>>();
	
	public void setRelatedDocScore(String origDoc, String relatedDoc, double score) {
		if (!relatedDocScores.containsKey(origDoc)) {
			relatedDocScores.put(origDoc, new HashMap<String, Double>());
		}
		relatedDocScores.get(origDoc).put(relatedDoc, score);
	}
	
	/**
	 * @param docno Docno of original doc.
	 * @return <docno, score> map for all docs related to the original doc, or null if none exist.
	 */
	public Map<String, Double> getDocsRelatedTo(String docno) {
		return relatedDocScores.get(docno);
	}
	
	public int getNumClusters() {
		return relatedDocScores.size();
	}
	
}
