package edu.gslis.category;

import edu.gslis.textrepresentation.FeatureVector;

public interface CategoryModel {
	
	public void build();
	
	public FeatureVector getModel(); 

}
