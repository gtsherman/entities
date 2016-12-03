package edu.gslis.entities.docscoring;

import java.util.Map;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.creators.DirichletDocScorerCreator;
import edu.gslis.searchhits.IndexBackedSearchHit;
import edu.gslis.searchhits.SearchHit;

/**
 * Handles the sum over expansion docs, i.e. Sum_e P(q|e)P(e|D)
 * @author Garrick
 *
 */
public class ExpansionDocsDocScorer implements DocScorer {
	
	public static final double DEFAULT_MU = 2500;
	
	private SearchHit doc;
	private IndexWrapper expansionIndex;
	private RelatedDocs clusters;
	
	private DirichletDocScorerCreator scorerCreator;
	
	public ExpansionDocsDocScorer(SearchHit origDoc, IndexWrapper expansionIndex, RelatedDocs clusters) {
		this(DEFAULT_MU, origDoc, expansionIndex, clusters);
	}
	
	public ExpansionDocsDocScorer(double mu, SearchHit origDoc, IndexWrapper expansionIndex, RelatedDocs clusters) {
		setDoc(origDoc);
		this.expansionIndex = expansionIndex;
		this.clusters = clusters;
		
		IndexBackedCollectionStats colStats = new IndexBackedCollectionStats();
		colStats.setStatSource(expansionIndex);

		scorerCreator = new DirichletDocScorerCreator(mu, colStats);
	}

	public void setDoc(SearchHit doc) {
		this.doc = doc;
	}
	
	@Override
	public double scoreTerm(String term) {
		Map<String, Double> relatedDocs = clusters.getDocsRelatedTo(doc.getDocno());

		double total = 0.0;
		if (relatedDocs == null) {
			System.err.println("No related docs for "+doc.getDocno());
			return total;
		}

		for (String docno : relatedDocs.keySet()) {
			SearchHit expDoc = new IndexBackedSearchHit(expansionIndex);
			expDoc.setDocno(docno);

			DocScorer expScorer = new DocScorerWithExpansionPrior(expDoc, scorerCreator.getDocScorer(expDoc), relatedDocs);
			total += expScorer.scoreTerm(term);
		}
		
		return total;
	}

}
