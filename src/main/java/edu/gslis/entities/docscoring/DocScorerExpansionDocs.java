package edu.gslis.entities.docscoring;

import java.util.Map;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.related_docs.DocumentClusterReader;
import edu.gslis.searchhits.SearchHit;

/**
 * Handles the sum over expansion docs, i.e. Sum_e P(q|e)P(e|D)
 * @author garrick
 *
 */
public class DocScorerExpansionDocs extends DocScorer {
	
	private SearchHit doc;
	private IndexWrapperIndriImpl expansionIndex;
	private DocumentClusterReader clusters;
	
	public DocScorerExpansionDocs(SearchHit origDoc, IndexWrapperIndriImpl expansionIndex, DocumentClusterReader clusters) {
		setDoc(origDoc);
		this.clusters = clusters;
	}

	public void setDoc(SearchHit doc) {
		this.doc = doc;
	}
	
	@Override
	public double scoreTerm(String term) {
		Map<String, Double> relatedDocs = clusters.getRelatedDocs(doc);

		IndexBackedCollectionStats colStats = new IndexBackedCollectionStats();
		colStats.setStatSource(expansionIndex);

		double total = 0.0;
		if (relatedDocs == null) {
			return total;
		}

		for (String docno : relatedDocs.keySet()) {
			SearchHit expDoc = new SearchHit();
			expDoc.setDocno(docno);
			expDoc.setFeatureVector(expansionIndex.getDocVector(docno, null));

			DocScorer expScorer = new DocScorerWithExpansionPrior(expDoc, new DocScorerDirichlet(expDoc, colStats), relatedDocs);
			total += expScorer.scoreTerm(term);
		}
		
		return total;
	}

}
