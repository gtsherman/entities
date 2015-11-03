package edu.gslis.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Category {
	
	private String name;
	private Categories subcats;
	private List<String> pages;

	
	public Category(String name) {
		this(name, new Categories());
	}
	
	public Category(String name, Categories subcats) {
		this(name, subcats, new ArrayList<String>());
	}
	
	/**
	 * Main constructor for category container class.
	 * @param name		The category title.
	 * @param subcats	The list of categories that are contained within this category.
	 * @param pages		The list of pages tagged with this category. This should not contain the pages contained by the subcategories.
	 */
	public Category(String name, Categories subcats, List<String> pages) {
		this.name = name;
		this.subcats = subcats;
		this.pages = pages;
	}
	

	public void addPage(String page) {
		this.pages.add(page);
	}
	
	public void addPages(List<String> pages) {
		this.pages.addAll(pages);
	}
	
	public List<String> getPages() {
		return this.pages;
	}
	
	/**
	 * Find the titles of the pages tagged with this category and its subcategories.
	 * @param depth				Specifies how many subcategories to traverse. Set to -1 for maximum traversal. 
	 * @return					A map associating each subcategory with a set of page titles for this category and the specified depth of subcategories
	 */
	public Map<Category, Set<String>> getAllSubpages(int depth) {
		Map<Category, Set<String>> pages = new HashMap<Category, Set<String>>();
		this.getAllSubpages(pages, depth);
		return pages;
	}
	
	private void getAllSubpages(Map<Category, Set<String>> pages, int depth) {
		if (pages.keySet().contains(this)) {
			return;
		}
		
		// Initialize pages with all the pages from this category
		pages.put(this, new HashSet<String>(this.pages));
		
		if (depth != 0) {
			Iterator<Category> it = this.subcats.iterator();
			while (it.hasNext()) {
				Category cat = it.next();
				cat.getAllSubpages(pages, depth--);
			}
		}
	}
	
	public void addSubcategory(Category subcat) {
		this.subcats.addCategory(subcat);
	}
	
	public void addSubcategories(List<Category> subcats) {
		this.subcats.addCategories(subcats);
	}
	
	public Categories getSubcategories() {
		return this.subcats;
	}
	
	
	public String getName() {
		return this.name;
	}
}
