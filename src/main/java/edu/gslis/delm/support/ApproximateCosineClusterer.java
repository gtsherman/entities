package edu.gslis.delm.support;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.similarity.CosineSimilarityScorer;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class ApproximateCosineClusterer {
	
	final static Logger logger = LoggerFactory.getLogger(ApproximateCosineClusterer.class);
	
	private final int DEFAULT_CLUSTER_SIZE = 100;
	
	private SearchHit document;
	private IndexWrapper index;
	private Stopper stopper;
	
	public ApproximateCosineClusterer(SearchHit doc, IndexWrapper index, Stopper stopper) {
		setDocument(doc);
		this.index = index;
		this.stopper = stopper;
	}
	
	public void setDocument(SearchHit doc) {
		this.document = doc;
	}
	
	public SearchHit getDocument() {
		return this.document;
	}
	
	/**
	 * Clusters documents by using cosine similarity to rescore an initial retrieval of 3000 documents.
	 * This is slow an approximate, but much faster than having to precompute the cosine similarity between every pair of documents in the collection.
	 * @param m The number of documents to include in the cluster
	 * @return A SearchHits object containing the clustered documents
	 */
	public SearchHits cluster(int m) {
		// Convert document into a general query:
		FeatureVector docVector = document.getFeatureVector();
		docVector.clip(20);
		GQuery query = new GQuery();
		query.setFeatureVector(docVector);
		query.applyStopper(stopper);
		
		// Get an initial retrieval that is most likely to be similar
		SearchHits results = index.runQuery(query, 3000);
		
		logger.debug("# Results for "+document.getDocno()+": "+results.size());
		
		// Rescore results with cosine scorer
		Iterator<SearchHit> resultIt = results.iterator();
		while (resultIt.hasNext()) {
			SearchHit result = resultIt.next();
			result.setFeatureVector(index.getDocVector(result.getDocID(), null));
			
			if (result.getDocno().equals(document.getDocno())) {
				logger.debug("Skipping self");
				continue;
			}
			
			CosineSimilarityScorer cosineScorer = new CosineSimilarityScorer(document.getFeatureVector(), result.getFeatureVector());
			double cosineScore = cosineScorer.score();
			
			result.setScore(cosineScore);
		}
		
		results.rank();
		results.crop(m);
		
		logger.debug("# in cluster:"+results.size());
		return results;
	}
	
	public SearchHits cluster() {
		return cluster(DEFAULT_CLUSTER_SIZE);
	}

}
