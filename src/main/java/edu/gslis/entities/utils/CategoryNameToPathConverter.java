package edu.gslis.entities.utils;

public class CategoryNameToPathConverter {
	
	private NameToFileNameConverter nc;
	
	public CategoryNameToPathConverter(String category) {
		nc = new NameToFileNameConverter(category);
	}
	
	public String getPath() {
		return nc.getFirstChar()+"/"+nc.getSecondChar()+"/"+nc.getName();
	}

}
