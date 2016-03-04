package edu.gslis.readers;

import java.io.File;

public abstract class AbstractReader implements Reader {
	
	protected String basePath = ".";

	public void readFileAbsolute(String file) {
		readFile(new File(file));
	}

	public abstract void readFile(File file);

	public void readFileRelative(String file) {
		readFile(new File(basePath+"/"+file));
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
}
