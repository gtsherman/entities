package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityExpectedProbability implements EntityProbability {
	private String thisClass = "[EntityQueryWeightedProbability] "; 

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
		
		System.err.println(thisClass+"Document: "+document.getDocno());
		System.err.println("\t"+thisClass+"There are "+entities.size()+" entities for this document");
	}
	
	public void setCollectionStats(CollectionStats cs) {
		this.cs = cs;
	}
	
	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();

		Set<String> termSet = new HashSet<String>(terms);
		Set<String> entitySet = new HashSet<String>(entities);

		System.err.println(thisClass+"Using index.");
		
		Map<String, FeatureVector> entityVecs = new HashMap<String, FeatureVector>();
		for (String entity : entitySet) {
			entityVecs.put(entity, index.getDocVector(entity, stopper));
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

				System.err.println(thisClass+term+": "+entityVector.getFeatureWeight(term));
				System.err.println(thisClass+"\tdoclength: "+entityVector.getLength());

				double qlscore = (entityVector.getFeatureWeight(term) + mu*collectionScore) / (entityVector.getLength() + mu);
				double confidence = de.getEntityConfidence(docno, entity);
				System.err.println(thisClass+"\tstarting confidence: "+confidence);
				if (confidence < 0.0) {
					confidence = Math.exp(confidence);
				}

				System.err.println(thisClass+"\tfinal confidence: "+confidence);
				System.err.println(thisClass+"\tfinal: "+qlscore*confidence);
				
				termProbs.put(term, termProbs.get(term)+(qlscore*confidence));
			}
		}
		
		return termProbs;
		
	}
}
