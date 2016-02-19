package edu.gslis.entities.docscoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.QueryDocScorer;
import edu.gslis.entities.docscoring.support.CategoryProbability;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

public class ScorerDirichletCategory2 extends QueryDocScorer {
	
	private static String thisClass = "[ScorerDirichletCategory] ";

	public String PARAMETER_NAME = "mu";
	public String BACKGROUND_MIX = "lambda";
	public double EPSILON = 1.0;
	
	private CategoryProbability catProb;
	
	private Map<Double, Double> lambdaToScore;
	
	public ScorerDirichletCategory2() {
		setParameter(PARAMETER_NAME, 2500);
	}

	public void setQuery(GQuery query) {
		this.gQuery = query;
	}
	
	public void setCategoryProbability(CategoryProbability cp) {
		this.catProb = cp;
	}

	public double score(SearchHit doc) {
		Map<String, Double> termProbs = new HashMap<String, Double>();
		Iterator<String> qit = gQuery.getFeatureVector().iterator();
		while (qit.hasNext()) {
			termProbs.put(qit.next(), 0.0);
		}

		this.catProb.setDocument(doc);
		try {
			termProbs = this.catProb.getProbability(new ArrayList<String>(termProbs.keySet()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<Double, Double> lambdaToScore = new HashMap<Double, Double>();
		for (double lambda = 0.0; lambda <= 1.0; lambda += 0.1) {
			double logLikelihood = 0.0;
			Iterator<String> queryIterator = gQuery.getFeatureVector().iterator();
			while(queryIterator.hasNext()) {
				String feature = queryIterator.next();
				double docFreq = doc.getFeatureVector().getFeatureWeight(feature);
				double docLength = doc.getLength();
				double categoryProb = termProbs.get(feature);
				System.err.println("\t\t\t"+thisClass+"Probability for term "+feature+": "+categoryProb);
				double collectionProb = (EPSILON + collectionStats.termCount(feature)) / collectionStats.getTokCount();
				double pr = (1-lambda)*((docFreq + 
						paramTable.get(PARAMETER_NAME)*collectionProb) / (docLength + paramTable.get(PARAMETER_NAME))) +
						(lambda)*categoryProb;
				double queryWeight = gQuery.getFeatureVector().getFeatureWeight(feature);
				logLikelihood += queryWeight * Math.log(pr);
			}
			System.err.println(thisClass+" "+doc.getDocno()+", "+lambda+": "+logLikelihood);
			lambdaToScore.put(lambda, logLikelihood);
		}
		setLambdaToScore(lambdaToScore);
		return 0.0;
	}
	
	private void setLambdaToScore(Map<Double, Double> lambdaToScore) {
		this.lambdaToScore = lambdaToScore;
	}
	
	public double getScore(double lambda) {
		return lambdaToScore.get(lambda);
	}
	
	public Map<Double, Double> getLambdaToScore() {
		return lambdaToScore;
	}

}
