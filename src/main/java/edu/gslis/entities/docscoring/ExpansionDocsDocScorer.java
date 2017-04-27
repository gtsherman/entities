package edu.gslis.entities.docscoring;

import java.util.Map;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;

/**
 * Handles the sum over expansion docs, i.e. Sum_e P(q|e)P(e|D)
 * @author Garrick
 *
 */
public class ExpansionDocsDocScorer implements DocScorer {
	
	public static final double DEFAULT_MU = 2500;
	
	private double mu;
	private IndexWrapper expansionIndex;
	private RelatedDocs clusters;
	
	private DocScorer dirichletScorer;
	
	public ExpansionDocsDocScorer(IndexWrapper expansionIndex, RelatedDocs clusters) {
		this(DEFAULT_MU, expansionIndex, clusters);
	}
	
	public ExpansionDocsDocScorer(double mu, IndexWrapper expansionIndex, RelatedDocs clusters) {
		this.expansionIndex = expansionIndex;
		this.clusters = clusters;
		this.mu = mu;
		
		IndexBackedCollectionStats colStats = new IndexBackedCollectionStats();
		colStats.setStatSource(expansionIndex);

		dirichletScorer = new CachedDocScorer(new DirichletDocScorer(mu, colStats));
	}
	
	public double getMu() {
		return mu;
	}
	
	public IndexWrapper getExpansionIndex() {
		return expansionIndex;
	}
	
	public RelatedDocs getClusters() {
		return clusters;
	}
	
	@Override
	public double scoreTerm(String term, SearchHit origDoc) {
		Map<String, Double> relatedDocs = clusters.getDocsRelatedTo(origDoc.getDocno());

		double total = 0.0;
		if (relatedDocs == null) {
			System.err.println("No related docs for "+origDoc.getDocno());
			return total;
		}

		for (String docno : relatedDocs.keySet()) {
			SearchHit expDoc = new IndexBackedSearchHit(expansionIndex);
			expDoc.setDocno(docno);
			
			DocScorer expScorer = new DocScorerWithExpansionPrior(expDoc, dirichletScorer, relatedDocs);
			//System.err.println("Term " + term + " in expansion doc " + docno + " (" + origDoc.getDocno() + "): " + expScorer.scoreTerm(term, expDoc));

			total += expScorer.scoreTerm(term, expDoc);
		}
		
		return total;
	}

}
