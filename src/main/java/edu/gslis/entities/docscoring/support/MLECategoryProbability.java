package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.categories.MLECategoryModel;
import edu.gslis.searchhits.SearchHit;

public class MLECategoryProbability implements CategoryProbability {

	private MLECategoryModel cm;

	public MLECategoryProbability(DocumentEntities de, EntityCategories ec, CategoryModel cm) {
		try {
			this.cm = (MLECategoryModel) cm;
			this.cm.setDocumentEntities(de);
			this.cm.setEntityCategories(ec);
		} catch (Exception e) {
			System.err.println("[MLECategoryProbability] Probably didn't supply an MLECategoryModel to MLECategoryProbability");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void setDocument(SearchHit document) {
		cm.setDocument(document);
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Map<String, Double> termProbs = new HashMap<String, Double>();

		Set<String> termSet = new HashSet<String>(terms);
		
		for (String term : termSet) {
			termProbs.put(term, cm.getScore(term));
		}
		
		return termProbs;
	}

}
