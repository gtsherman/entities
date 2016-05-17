package edu.gslis.delm.support;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class ClusteredDocumentConfidenceScorer {
	
	final static Logger logger = LoggerFactory.getLogger(ClusteredDocumentConfidenceScorer.class);
	
	private SearchHits cluster;
	private double totalCosine;
	
	public ClusteredDocumentConfidenceScorer(SearchHits cluster) {
		setCluster(cluster);
	}
	
	public void setCluster(SearchHits cluster) {
		this.cluster = cluster;
		
		// Compute totalCosine
		totalCosine = 0;
		Iterator<SearchHit> docIt = this.cluster.iterator();
		while (docIt.hasNext()) {
			SearchHit doc = docIt.next();
			totalCosine += doc.getScore();
		}
		logger.debug("Total cosine for cluster: "+totalCosine);
	}
	
	public SearchHits computeConfidenceScores() {
		SearchHits confidenceCluster = new SearchHits();

		Iterator<SearchHit> docIt = cluster.iterator();
		while (docIt.hasNext()) {
			SearchHit doc = docIt.next();
			
			SearchHit confidenceDoc = new SearchHit();
			confidenceDoc.setDocID(doc.getDocID());
			confidenceDoc.setDocno(doc.getDocno());
			confidenceDoc.setFeatureVector(doc.getFeatureVector());
			confidenceDoc.setLength(doc.getLength());

			confidenceDoc.setScore(getConfidenceOf(doc));

			confidenceCluster.add(confidenceDoc);
		}
		return confidenceCluster;
	}
	
	private double getConfidenceOf(SearchHit doc) {
		return doc.getScore() / totalCosine;
	}

}
