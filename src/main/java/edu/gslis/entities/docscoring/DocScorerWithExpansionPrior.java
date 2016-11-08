package edu.gslis.entities.docscoring;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.searchhits.SearchHit;

/**
 * Handles the P(q|e)P(e|D) part of the expansion process.
 * @author garrick
 *
 */
public class DocScorerWithExpansionPrior extends DocScorerWithDocumentPrior {
	
	private SearchHit doc;
	private Map<String, Double> relatedDocs;
	
	/**
	 * @param expansionDoc The expansion document needing a prior.
	 * @param baseScorer The base scorer, probably DocScorerDirichlet, to handle P(q|e).
	 * @param relatedDocs A <string, double> map of all expansion docs and their scores.
	 */
	public DocScorerWithExpansionPrior(SearchHit expansionDoc, DocScorer baseScorer, Map<String, Double> relatedDocs) {
		super(baseScorer);
		setDoc(expansionDoc);
		this.relatedDocs = normalizeScores(relatedDocs);
	}
	
	public void setDoc(SearchHit doc) {
		this.doc = doc;
	}
	
	@Override
	public double getPrior() {
		if (!relatedDocs.containsKey(doc.getDocno())) {
			return 0.0;
		}
		return relatedDocs.get(doc.getDocno());
	}
	
	private Map<String, Double> normalizeScores(Map<String, Double> relatedDocs) {
		Map<String, Double> normalized = new HashMap<String, Double>();
		if (relatedDocs != null) {
			double total = 0.0;
			for (String docno : relatedDocs.keySet()) {
				total += relatedDocs.get(docno);
			}
			for (String docno : relatedDocs.keySet()) {
				normalized.put(docno, relatedDocs.get(docno) / total);
			}
		}
		return normalized;
	}

}
