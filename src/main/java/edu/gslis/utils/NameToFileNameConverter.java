package edu.gslis.utils;

public class NameToFileNameConverter {
	
	private String name;
	
	public String REPLACEMENT_CHAR = "_";
	
	public NameToFileNameConverter(String name) {
		this.setName(name);
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getFirstChar() {
		if (this.name.length() == 0) {
			return this.REPLACEMENT_CHAR;
		}
		return Character.toString(this.name.charAt(0));
	}
	
	public String getSecondChar() {
		if (this.name.length() > 1 && this.name.charAt(1) != '/') {
			return Character.toString(this.name.charAt(1));
		}
		return this.REPLACEMENT_CHAR;
	}

}
