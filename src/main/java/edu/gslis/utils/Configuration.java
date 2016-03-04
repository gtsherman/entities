package edu.gslis.utils;

public interface Configuration {
	
	public void read(String file);
	
	public String get(String key);
	
	public void set(String key, String value);

}
