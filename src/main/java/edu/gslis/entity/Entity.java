package edu.gslis.entity;

import edu.gslis.category.Categories;
import edu.gslis.searchhits.SearchHit;

public class Entity {

	String title;
	SearchHit hit;
	Categories categories;
	
	public Entity(String title, SearchHit hit) {
		this(title, hit, new Categories());
	}
	
	public Entity(String title, SearchHit hit, Categories categories) {
		this.title = title;
		this.hit = hit;
		this.categories = categories;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public SearchHit getContent() {
		return this.hit;
	}
}
