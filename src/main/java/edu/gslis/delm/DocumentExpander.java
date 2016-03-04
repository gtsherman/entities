package edu.gslis.delm;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.delm.support.ApproximateCosineClusterer;
import edu.gslis.delm.support.ClusteredDocumentConfidenceScorer;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class DocumentExpander {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentExpander.class);
	
	private IndexWrapper index;
	private Stopper stopper;
	
	private FeatureVector pseudoDocVector;
	
	public DocumentExpander(IndexWrapper index, Stopper stopper) {
		this.index = index;
		this.stopper = stopper;
	}
	
	public void expand(SearchHit document, double alpha) {
		if (document == null) {
			logger.warn("You have not specified a document to expand.");
			return;
		}

		logger.info("Clustering documents around "+document.getDocno());
		ApproximateCosineClusterer clusterer = new ApproximateCosineClusterer(document, index, stopper);
		SearchHits clusters = clusterer.cluster();
		
		logger.info("Computing confidence scores for clustered documents");
		ClusteredDocumentConfidenceScorer confidenceScorer = new ClusteredDocumentConfidenceScorer(clusters);
		SearchHits confidenceCluster = confidenceScorer.computeConfidenceScores();
		
		// Compute the cluster pseudo-count
		FeatureVector clusterVector = computeClusterPseudoCount(confidenceCluster);

		// Combine the document and cluster vectors, mixed by alpha
		combineDocumentAndClusterVectors(document, clusterVector, alpha);
		
		logger.info("Completed expansion of "+document.getDocno());
	}
	
	public FeatureVector getPseudoDocVector() {
		return pseudoDocVector;
	}
	
	/**
	 * The summation over cluster documents.
	 * @param confidenceCluster SearchHits containing cluster docs with confidence scores
	 * @return The cluster pseudo-counts
	 */
	private FeatureVector computeClusterPseudoCount(SearchHits confidenceCluster) {
		logger.info("Computing cluster pseudo-count");

		FeatureVector clusterVector = new FeatureVector(null);
		Iterator<SearchHit> clusterIt = confidenceCluster.iterator();
		while (clusterIt.hasNext()) {
			SearchHit clusterDoc = clusterIt.next();
			
			Iterator<String> termIt = clusterDoc.getFeatureVector().iterator();
			while (termIt.hasNext()) {
				String term = termIt.next();
				
				double termCount = clusterDoc.getFeatureVector().getFeatureWeight(term);
				double confidence = clusterDoc.getScore();
				double clusterCount = confidence * termCount;

				clusterVector.addTerm(term, clusterCount);
			}
		}
		return clusterVector;
	}
	
	/**
	 * Mix the original document and cluster counts.
	 * @param document The original document, which we are expanding
	 * @param clusterVector The vector containing cluster pseudo-counts
	 * @param alpha The mixing weight; alpha governs the original document weight, 1-alpha is the cluster weight
	 */
	private void combineDocumentAndClusterVectors(SearchHit document, FeatureVector clusterVector, double alpha) {
		logger.info("Combining document and cluster");
		
		pseudoDocVector = new FeatureVector(null);

		FeatureVector docVector = document.getFeatureVector();
		Iterator<String> termIt = clusterVector.iterator();
		while (termIt.hasNext()) {
			String term = termIt.next();

			double pseudoCount = alpha * docVector.getFeatureWeight(term) + 
					(1-alpha) * clusterVector.getFeatureWeight(term);

			pseudoDocVector.setTerm(term, pseudoCount);
		}
	}

}
