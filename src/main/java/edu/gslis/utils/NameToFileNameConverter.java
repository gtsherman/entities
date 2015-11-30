package edu.gslis.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NameToFileNameConverter {
	
	private String name;
	
	public List<String> TO_BE_REPLACED = new ArrayList<String>(Arrays.asList(".", "/", "\\"));
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
		return this.getChar(0);
	}
	
	public String getSecondChar() {
		return this.getChar(1);
	}
	
	private String getChar(int i) {
		if (this.name.length() > i && !this.TO_BE_REPLACED.contains(this.name.charAt(i))) {
			return Character.toString(this.name.charAt(i));
		}
		return this.REPLACEMENT_CHAR;
	}

}
