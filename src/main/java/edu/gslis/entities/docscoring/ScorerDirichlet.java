package edu.gslis.entities.docscoring;

import java.util.Iterator;

import edu.gslis.docscoring.QueryDocScorer;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

/**
 * Standard dirichlet query likelihood scorer
 * 
 * @author mefron
 *
 */
public class ScorerDirichlet extends QueryDocScorer {
	public String PARAMETER_NAME = "mu";
	public double EPSILON = 1.0;
	
	public ScorerDirichlet() {
		setParameter(PARAMETER_NAME, 2500);
	}
	public void setQuery(GQuery query) {
		this.gQuery = query;
	}
	
	/**
	 * retrieves the log-likelihood.  assumes the search hit is populated w term counts.
	 */
	public double score(SearchHit doc) {
		double logLikelihood = 0.0;
		Iterator<String> queryIterator = gQuery.getFeatureVector().iterator();
		while(queryIterator.hasNext()) {
			String feature = queryIterator.next();
			double docFreq = doc.getFeatureVector().getFeatureWeight(feature);
			double docLength = doc.getLength();
			double collectionProb = (EPSILON + collectionStats.termCount(feature)) / collectionStats.getTokCount();
			double pr = (docFreq + 
					paramTable.get(PARAMETER_NAME)*collectionProb) / 
					(docLength + paramTable.get(PARAMETER_NAME));
			double queryWeight = gQuery.getFeatureVector().getFeatureWeight(feature);
			System.err.println(doc.getDocno());
			System.err.println("\tterm: "+feature);
			System.err.println("\t\t\tdocFreq: "+docFreq);
			System.err.println("\t\t\tcollection prob: "+collectionProb);
			System.err.println("\t\t\tdoclength: "+docLength);

			logLikelihood += queryWeight * Math.log(pr);
		}
		return logLikelihood;
	}
	

	


	
	


}
