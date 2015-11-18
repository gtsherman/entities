package edu.gslis.entities.readers;

import java.io.File;

public interface Reader {
	
	public void readFileAbsolute(String file);
	
	public void readFile(File file);
	
	public void readFileRelative(String file);
	
	public void setBasePath(String basePath);
	
}
