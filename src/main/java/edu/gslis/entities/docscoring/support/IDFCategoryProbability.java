package edu.gslis.entities.docscoring.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.PrecomputedCategoryModel;
import edu.gslis.entities.utils.CategoryNameToPathConverter;
import edu.gslis.searchhits.SearchHit;

public class IDFCategoryProbability implements CategoryProbability {

	private DocumentEntities de;
	private PrecomputedCategoryModel cm;
	private EntityCategories ec;
	
	private final double EPSILON = 0.001;
	
	/**
	 * Constructor method.
	 * @param de	A DocumentEntities object with basePath specified.
	 * @param ec	An EntityCategories object with entity categories already read in.
	 * @param cm	A CategoryModel object with basePath specified.
	 */
	public IDFCategoryProbability(DocumentEntities de, EntityCategories ec, PrecomputedCategoryModel cm) {
		this.de = de;
		this.ec = ec;
		this.cm = cm;
	}

	public void setDocument(SearchHit doc) {
		de.readFileRelative(doc.getDocno()+".tsv");
	}

	public Map<String, Double> getProbability(List<String> terms) {
		// Initialize the term probabilities
		Map<String, List<Double>> termProbs = new HashMap<String, List<Double>>();
		for (String term : terms) {
			termProbs.put(term, new ArrayList<Double>());
		}
		
		// Iterate through entities in the document
		// (the document has been specified in the DocumentEntities object)
		for (String entity : de.getEntities()) {
			System.err.println("\tEntity: "+entity);

			// Find the IDF category for this entity
			String cat = "";
			int catSize = Integer.MAX_VALUE;
			for (String category : ec.getCategories(entity)) {
				System.err.println("\t\tCategory: "+category);
				Set<String> catEntities = ec.getEntities(category);
				System.err.println("\t\t\tSize:"+catEntities.size());
				if (catEntities.size() < catSize) {
					cat = category;
					catSize = catEntities.size();
				}
			}
			
			// Find the term probabilities in the IDF category
			CategoryNameToPathConverter cp = new CategoryNameToPathConverter(cat);
			System.err.println("\tReading least encompassing category model file: "+cat);
			cm.readFileRelative(cp.getPath());
			for (String term : terms) {
				double score = cm.getScore(term);
				termProbs.get(term).add(score);
			}
		}
		
		// Combine the probabilities from the various entities
		Map<String, Double> probs = new HashMap<String, Double>();
		for (String term : terms) {
			List<Double> probList = termProbs.get(term);
			double prob = this.EPSILON; 
			if (probList.size() > 0) {
				prob = probList.get(0);
				for (int i = 1; i < probList.size(); i++) {
					prob *= probList.get(i);
				}
			}
			probs.put(term, prob);	
		}
		
		return probs;
	}
	
}