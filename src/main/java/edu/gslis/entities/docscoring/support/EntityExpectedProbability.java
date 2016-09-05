package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.readers.DocumentEntityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityExpectedProbability implements EntityProbability {
	
	final static Logger logger = LoggerFactory.getLogger(EntityExpectedProbability.class);

	private DocumentEntityReader de;
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	private List<String> entities;
	private CollectionStats cs;
	private String docno;
	
	public EntityExpectedProbability(DocumentEntityReader de, IndexWrapperIndriImpl index, Stopper stopper) {
		this.de = de;
		this.index = index;
		this.stopper = stopper;
	}
	
	public void setDocument(SearchHit document) {
		docno = document.getDocno();
		entities = de.getEntities(docno);
		
		logger.info("Document: "+document.getDocno());
		logger.debug("There are "+entities.size()+" entities for this document");
	}
	
	public void setCollectionStats(CollectionStats cs) {
		this.cs = cs;
	}
	
	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();

		Set<String> termSet = new HashSet<String>(terms);
		Set<String> entitySet = new HashSet<String>(entities);

		logger.debug("Using index.");
		
		Map<String, FeatureVector> entityVecs = new HashMap<String, FeatureVector>();
		Map<String, Double> normalizedConfidences = new HashMap<String, Double>();
		double totalConfidence = 0.0;
		for (String entity : entitySet) {
			entityVecs.put(entity, index.getDocVector(entity, stopper));

			double confidence = de.getEntityConfidence(docno, entity);
			normalizedConfidences.put(entity, confidence);
			totalConfidence += confidence;
		}
		
		for (String entity : normalizedConfidences.keySet()) {
			double initialConfidence = normalizedConfidences.get(entity);
			normalizedConfidences.put(entity, initialConfidence / totalConfidence);
		}

		for (String term : termSet) {
			if (!termProbs.containsKey(term)) {
				termProbs.put(term, 0.0);
			}
			
			double mu = 0.0;
			double collectionScore = 0.0;
			if (cs != null) {
				collectionScore = (1.0 + cs.termCount(term)) / cs.getTokCount();
				mu = 2500;
			}

			FeatureVector entityVector;
			for (String entity : entitySet) {
				entityVector = entityVecs.get(entity);
				
				logger.debug("Entity: "+entity);

				logger.debug(term+": "+entityVector.getFeatureWeight(term));
				logger.debug("Document length: "+entityVector.getLength());

				double qlscore = (entityVector.getFeatureWeight(term) + mu*collectionScore) / (entityVector.getLength() + mu);
				double confidence = normalizedConfidences.get(entity);
				
				logger.debug("Final score: "+qlscore*confidence);
				
				termProbs.put(term, termProbs.get(term)+(qlscore*confidence));
			}
		}
		
		return termProbs;
		
	}
}
