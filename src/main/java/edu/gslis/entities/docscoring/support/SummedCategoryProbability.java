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

public class SummedCategoryProbability implements CategoryProbability {

	private DocumentEntities de;
	private PrecomputedCategoryModel cm;
	private EntityCategories ec;
	
	/**
	 * Constructor method.
	 * @param de	A DocumentEntities object with basePath specified.
	 * @param ec	An EntityCategories object with entity categories already read in.
	 * @param cm	A CategoryModel object with basePath specified.
	 */
	public SummedCategoryProbability(DocumentEntities de, EntityCategories ec, PrecomputedCategoryModel cm) {
		this.de = de;
		this.ec = ec;
		this.cm = cm;
	}

	public void setDocument(SearchHit doc) {
		de.readFileRelative(doc.getDocno()+".tsv");
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Set<String> termSet = new HashSet<String>(terms);
		
		Map<String, Double> termScores = new HashMap<String, Double>();
		for (String term : terms) {
			termScores.put(term, 0.0);
		}

		Set<String> seenCategories = new HashSet<String>(); // for efficiency
		for (String entity : de.getEntities()) {
			for (String category : ec.getCategories(entity)) {
				if (!seenCategories.contains(category)) {  // if we haven't seen this category before
					seenCategories.add(category);

					CategoryNameToPathConverter cp = new CategoryNameToPathConverter(category);
					try {
						cm.readFileRelative(cp.getPath());

						for (String term : termSet) {
							double currentScore = termScores.get(term);
							double newScore = currentScore + cm.getScore(term);

							termScores.put(term, newScore);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return termScores;
	}

}
