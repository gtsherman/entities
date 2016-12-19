package edu.gslis.entities.docscoring;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.keyvalue.MultiKey;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.related_docs.RelatedDocs;
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
	
	private IndexWrapper expansionIndex;
	private RelatedDocs clusters;
	
	private DirichletDocScorer scorer;
	
	private LoadingCache<MultiKey, Double> termScores = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<MultiKey, Double>() {
						public Double load(MultiKey key) throws Exception {
							String term = (String) key.getKey(TERM_KEY_INDEX);
							SearchHit origDoc = (SearchHit) key.getKey(DOC_KEY_INDEX);
							
							Map<String, Double> relatedDocs = clusters.getDocsRelatedTo(origDoc.getDocno());

							double total = 0.0;
							if (relatedDocs == null) {
								System.err.println("No related docs for "+origDoc.getDocno());
								return total;
							}

							for (String docno : relatedDocs.keySet()) {
								SearchHit expDoc = new IndexBackedSearchHit(expansionIndex);
								expDoc.setDocno(docno);

								DocScorer expScorer = new DocScorerWithExpansionPrior(expDoc, scorer, relatedDocs);
								total += expScorer.scoreTerm(term, expDoc);
							}
							
							return total;
						}
					});
	
	public ExpansionDocsDocScorer(IndexWrapper expansionIndex, RelatedDocs clusters) {
		this(DEFAULT_MU, expansionIndex, clusters);
	}
	
	public ExpansionDocsDocScorer(double mu, IndexWrapper expansionIndex, RelatedDocs clusters) {
		this.expansionIndex = expansionIndex;
		this.clusters = clusters;
		
		IndexBackedCollectionStats colStats = new IndexBackedCollectionStats();
		colStats.setStatSource(expansionIndex);

		scorer = new DirichletDocScorer(mu, colStats);
	}
	
	public IndexWrapper getExpansionIndex() {
		return expansionIndex;
	}
	
	public RelatedDocs getClusters() {
		return clusters;
	}
	
	@Override
	public double scoreTerm(String term, SearchHit origDoc) {
		// Setup keys
		Object[] keys = new Object[2];
		keys[TERM_KEY_INDEX] = term;
		keys[DOC_KEY_INDEX] = origDoc;
		
		// Convert to MultiKey
		MultiKey key = new MultiKey(keys);
		
		// Lookup in cache
		try {
			return termScores.get(key);
		} catch (ExecutionException e) {
			System.err.println("Error scoring term '" + term +
					"' in document '" + origDoc.getDocno() + "'");
			System.err.println(e.getStackTrace());
		}
		
		// Default to zero, if we have an issue
		return 0.0;
	}

}
