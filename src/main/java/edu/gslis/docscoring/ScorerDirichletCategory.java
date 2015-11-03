package edu.gslis.docscoring;

import java.util.Iterator;

import edu.gslis.docscoring.support.CategoryProbability;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

public class ScorerDirichletCategory extends QueryDocScorer {

	public String PARAMETER_NAME = "mu";
	public String BACKGROUND_MIX = "lambda";
	public double EPSILON = 1.0;
	
	private CategoryProbability catProb;
	
	public ScorerDirichletCategory() {
		setParameter(PARAMETER_NAME, 2500);
	}

	public void setQuery(GQuery query) {
		this.gQuery = query;
	}
	
	public void setCategoryProbability(CategoryProbability cp) {
		this.catProb = cp;
	}

	public double score(SearchHit doc) {
		this.catProb.setDoc(doc);

		double logLikelihood = 0.0;
		Iterator<String> queryIterator = gQuery.getFeatureVector().iterator();
		while(queryIterator.hasNext()) {
			String feature = queryIterator.next();
			double docFreq = doc.getFeatureVector().getFeatureWeight(feature);
			double docLength = doc.getLength();
			double categoryProb = this.catProb.getProbability(feature);
			double collectionProb = (EPSILON + collectionStats.termCount(feature)) / collectionStats.getTokCount();
			double pr = (docFreq + 
					paramTable.get(PARAMETER_NAME)*(paramTable.get(BACKGROUND_MIX)*categoryProb + (1-paramTable.get(BACKGROUND_MIX))*collectionProb)) / 
					(docLength + paramTable.get(PARAMETER_NAME));
			double queryWeight = gQuery.getFeatureVector().getFeatureWeight(feature);
			logLikelihood += queryWeight * Math.log(pr);
		}
		return logLikelihood;
	}

}
