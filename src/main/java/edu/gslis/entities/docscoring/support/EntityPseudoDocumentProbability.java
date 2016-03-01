package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityPseudoDocumentProbability implements EntityProbability {
	
	final static Logger logger = LoggerFactory.getLogger(EntityPseudoDocumentProbability.class);

	private DocumentEntityReader de;
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	private List<String> entities;
	private CollectionStats cs;
	
	public EntityPseudoDocumentProbability(DocumentEntityReader de, IndexWrapperIndriImpl index, Stopper stopper) {
		this.de = de;
		this.index = index;
		this.stopper = stopper;
	}
	
	public void setDocument(SearchHit document) {
		entities = de.getEntities(document.getDocno());
		
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

		Map<String, FeatureVector> entityVectors = new HashMap<String, FeatureVector>();
		FeatureVector combinedEntityVector = new FeatureVector(stopper);
		for (String entity : entitySet) {
			if (!entityVectors.containsKey(entity)) {
				logger.debug("Getting vector for "+entity);
				entityVectors.put(entity, index.getDocVector(entity, stopper));
			}
			FeatureVector entityVector = entityVectors.get(entity);
			Iterator<String> vectorIt = entityVector.iterator();
			while(vectorIt.hasNext()) {
				String term = vectorIt.next();
				
				if (stopper != null && stopper.isStopWord(term))
					continue;
				
				combinedEntityVector.addTerm(term, entityVector.getFeatureWeight(term));
			}
		}
		
		for (String term : termSet) {
			double mu = 0.0;
			double collectionScore = 0.0;
			if (cs != null) {
				collectionScore = (1.0 + cs.termCount(term)) / cs.getTokCount();
				mu = 2500;
			}
			logger.debug(term+": "+combinedEntityVector.getFeatureWeight(term));
			logger.debug("doclength: "+combinedEntityVector.getLength());
			double score = (combinedEntityVector.getFeatureWeight(term) + mu*collectionScore) / (combinedEntityVector.getLength() + 1 + mu);
			termProbs.put(term, score);
		}
		
		logger.debug("Term probabilities:");
		for (String term : termProbs.keySet()) {
			logger.debug(term+" "+termProbs.get(term));
		}
			
		return termProbs;
	}
}
