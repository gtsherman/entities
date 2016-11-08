package edu.gslis.entities.docscoring;

import java.util.Iterator;

import edu.gslis.queries.GQuery;

/**
 * Computes score across all query terms, e.g. Prod_q (1-lam)P(q|D) + lam*Sum_e P(q|e)P(e|D)
 * @author garrick
 *
 */
public class QueryScorerQueryLikelihood implements QueryScorer {

	private DocScorer termScorer;
	
	public QueryScorerQueryLikelihood(DocScorer termScorer) {
		this.termScorer = termScorer;
	}
	
	public double scoreQuery(GQuery query) {
		double loglikelihood = 0.0;

		Iterator<String> termIt = query.getFeatureVector().iterator();
		while (termIt.hasNext()) {
			String term = termIt.next();
			double termProb = termScorer.scoreTerm(term);
			double qWeight = query.getFeatureVector().getFeatureWeight(term);
			loglikelihood += qWeight * Math.log(termProb);
		}
		
		return loglikelihood;
	}

}
