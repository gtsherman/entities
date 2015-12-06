package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.PrecomputedCategoryModel;
import edu.gslis.entities.utils.CategoryNameToPathConverter;
import edu.gslis.searchhits.SearchHit;

public class EntityWeightedCategoryProbability implements CategoryProbability {
	
	private static String thisClass = "[EntityWeightedCategoryProbability] ";

	private static double EPSILON = 1.0;

	private DocumentEntities de;
	private PrecomputedCategoryModel cm;
	private EntityCategories ec;
	
	/**
	 * Constructor method.
	 * @param de	A DocumentEntities object with basePath specified.
	 * @param ec	An EntityCategories object with entity categories already read in.
	 * @param cm	A CategoryModel object with basePath specified.
	 */
	public EntityWeightedCategoryProbability(DocumentEntities de, EntityCategories ec, PrecomputedCategoryModel cm) {
		this.de = de;
		this.ec = ec;
		this.cm = cm;
	}

	public void setDocument(SearchHit doc) {
		de.readFileRelative(doc.getDocno()+".tsv");
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();
		
		Set<String> termSet = new HashSet<String>(terms);
		for (String entity : de.getEntities()) {
			double entityWeight = de.getEntityFreq(entity) / (double) de.getNumberOfEntities();
			//System.err.println(thisClass+"Entity weight for "+entity+": "+entityWeight);

			for (String category : ec.getCategories(entity)) {
				CategoryNameToPathConverter cp = new CategoryNameToPathConverter(category);
				try {
					cm.readFileRelative(cp.getPath());

					for (String term : termSet) {
						double score = cm.getScore(term);
						if (score == 0.0) {
							score = EPSILON;
						}
						//System.err.println(thisClass+"Score for "+term+": "+score);
						score *= entityWeight;

						double newScore;
						Double oldScore = termProbs.get(term);
						if (oldScore == null) {
							newScore = score;
						} else {
							newScore = oldScore + score;
						}
						System.err.println(thisClass+"newScore for "+term+": "+newScore);
						termProbs.put(term, newScore);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
		}
		
		for (String term : termSet) {
			if (termProbs.get(term) == null) {
				termProbs.put(term, 0.0);
			}
		}
		return termProbs;
	}

}
