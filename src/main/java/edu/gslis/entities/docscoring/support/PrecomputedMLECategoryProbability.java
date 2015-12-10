package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.entities.DocumentEntities;
import edu.gslis.entities.EntityCategories;
import edu.gslis.entities.categories.CategoryModel;
import edu.gslis.entities.categories.PrecomputedCategoryModel;
import edu.gslis.entities.utils.CategoryNameToPathConverter;
import edu.gslis.searchhits.SearchHit;

public class PrecomputedMLECategoryProbability implements CategoryProbability {
	
	private String thisClass = "[PrecomputedMLECategoryProbability] ";
	
	private DocumentEntities de;
	private EntityCategories ec;
	private CategoryModel cm;
	private CategoryLength cl;
	
	private double EPSILON = 1.0;
	
	public PrecomputedMLECategoryProbability(DocumentEntities de, EntityCategories ec, CategoryModel cm) {
		this.de = de;
		this.ec = ec;
		this.cm = cm;
	}

	public void setCategoryLength(CategoryLength cl) {
		this.cl = cl;
	}

	public void setDocument(SearchHit document) {
		de.readFileRelative(document.getDocno()+".tsv");
	}

	public Map<String, Double> getProbability(List<String> terms) {
		Set<String> termSet = new HashSet<String>(terms);
		
		Map<String, Double> termOccurrences = new HashMap<String, Double>();
		for (String term : termSet) {
			termOccurrences.put(term, 0.0);
		}

		Set<String> seenCats = new HashSet<String>();
		
		double totalCatLength = 0.0;
		for (String entity : de.getEntities()) {
			for (String category : ec.getCategories(entity)) {
				if (seenCats.contains(category))
					continue;
				seenCats.add(category);
				
				CategoryNameToPathConverter cp = new CategoryNameToPathConverter(category);
				((PrecomputedCategoryModel) cm).readFileRelative(cp.getPath());
				
				double catLength = cl.getLength(category);
				totalCatLength += catLength;
				for (String term : termSet) {
					double catScore = cm.getScore(term);
					double occurrences = catScore * catLength;
					System.err.println(thisClass+"Occurrences of "+term+": "+occurrences);
					termOccurrences.put(term, termOccurrences.get(term)+occurrences);
				}
			}
		}
		
		System.err.println(thisClass+"Computing probabilities:");
		for (String term : termSet) {
			double occurrences = termOccurrences.get(term);
			if (occurrences == 0.0) {
				occurrences = EPSILON;
				totalCatLength += 1;
			}
			System.err.println(thisClass+"\tTerm: "+term+"; prob: "+occurrences/totalCatLength);
			termOccurrences.put(term, occurrences/totalCatLength);
		}
		
		return termOccurrences;
	}

}
