package edu.gslis.entities.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleConfiguration implements Configuration {
	
	static final Logger logger = LoggerFactory.getLogger(SimpleConfiguration.class);

	private Map<String, String> config;
	
	public SimpleConfiguration() {
		this.config = new HashMap<String, String>();
	}
	
	public void read(String file) {
		try {
			Scanner scanner = new Scanner(new File(file));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String key = line.split(":")[0].trim();
				String value = line.substring(line.indexOf(key)+key.length()+1).trim();
				
				this.config.put(key, value);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			logger.error("Configuration file not found. A configuration file is required. Exiting...");
			System.exit(-1);
		}
	}

	public String get(String key) {
		return this.config.keySet().contains(key) ? this.config.get(key) : null;
	}

	public void set(String key, String value) {
		this.config.put(key, value);
	}

}
