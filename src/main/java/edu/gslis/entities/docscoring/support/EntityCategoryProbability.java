package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityCategoryProbability implements CategoryProbability {

	private DocumentEntityReader de;
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	private List<String> entities;
	
	public EntityCategoryProbability(DocumentEntityReader de, IndexWrapperIndriImpl index, Stopper stopper) {
		this.de = de;
		this.index = index;
		this.stopper = stopper;
	}
	
	public void setDocument(SearchHit document) {
		entities = de.getEntities(document.getDocno());
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();
		
		Set<String> termSet = new HashSet<String>(terms);
		Set<String> entitySet = new HashSet<String>(entities);
		
		Map<String, FeatureVector> entityVectors = new HashMap<String, FeatureVector>();
		FeatureVector combinedEntityVector = new FeatureVector(stopper);
		for (String entity : entitySet) {
			if (!entityVectors.containsKey(entity)) {
				entityVectors.put(entity, index.getDocVector(entity, stopper));
			}
			FeatureVector entityVector = entityVectors.get(entity);
			Iterator<String> vectorIt = entityVector.iterator();
			while(vectorIt.hasNext()) {
				String term = vectorIt.next();
				combinedEntityVector.addTerm(term, entityVector.getFeatureWeight(term));
			}
		}
		
		for (String term : termSet) {
			termProbs.put(term, combinedEntityVector.getFeatureWeight(term) / (combinedEntityVector.getLength()+1));
		}
		
		return termProbs;
	}

}
