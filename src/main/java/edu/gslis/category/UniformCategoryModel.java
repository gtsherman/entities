package edu.gslis.category;

import java.util.Map;
import java.util.Set;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.expansion.Feedback;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class UniformCategoryModel implements CategoryModel {

	private Category category;
	private int subcategoryDepth;
	
	private IndexWrapper index;
	private Stopper stopper;
	
	private Feedback model;
	
	/**
	 * Constructor method, defaults to only pages in this category (not subcategories)
	 * @param category	The category to use to construct the model
	 */
	public UniformCategoryModel(Category category) {
		this(category, 0);
	}

	/**
	 * Constructor method.
	 * @param category		The category to use to construct the model
	 * @param subcategoryDepth	The subcategory depth to use (-1 will recurse to maximum)
	 */
	public UniformCategoryModel(Category category, int subcategoryDepth) {
		this.category = category;
		this.subcategoryDepth = subcategoryDepth;
	}
	
	public void setIndex(IndexWrapper index) {
		this.index = index;
	}
	
	public void setStopper(Stopper stopper) {
		this.stopper = stopper;
	}

	/**
	 * Builds a "model" -- essentially a merged FeatureVector of the documents specified by the categories.
	 */
	public void build() {
		if (this.index == null) {
			throw new NullPointerException("Please specify an index before building model.");
		}

		Map<Category, Set<String>> categoryPages = this.category.getAllSubpages(this.subcategoryDepth);
		
		// Get page representations
		SearchHits pages = new SearchHits();
		for (Set<String> pageSet : categoryPages.values()) {
			for (String title : pageSet) {
				System.err.println("\tCategory contains page: "+title);
				if (title.startsWith("Super_")) {
					System.err.println("Skipping because of weird Super issue");
					continue;
				}
				try {
					SearchHit page = this.index.getSearchHit(title, this.stopper);
					page.setDocno(title);
					page.setScore(1.0);
					pages.add(page);
				} catch (Exception e) {
					System.err.println("\t\tIssue with "+title);
				}
			}
		}
		
		FeedbackRelevanceModel model = new FeedbackRelevanceModel();
		model.setIndex(this.index);
		model.setStopper(this.stopper);
		model.setRes(pages);
		model.build();
		
		this.model = model;
	}
	
	public FeatureVector getModel() {
		return this.model.asFeatureVector();
	}

}
