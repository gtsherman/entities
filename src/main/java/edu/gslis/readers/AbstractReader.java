package edu.gslis.readers;

import java.io.File;

public abstract class AbstractReader implements Reader {
	
	protected String basePath = ".";

	@Override
	public void readFileAbsolute(String file) {
		readFile(new File(file));
	}

	@Override
	public abstract void readFile(File file);

	@Override
	public void readFileRelative(String file) {
		readFile(new File(basePath+"/"+file));
	}

	@Override
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
}
