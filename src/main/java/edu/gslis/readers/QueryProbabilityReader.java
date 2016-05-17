package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryProbabilityReader extends AbstractReader {
	
	private static final Logger logger = LoggerFactory.getLogger(QueryProbabilityReader.class);

	private Map<String, Double> termProbs;
	
	@Override
	public void readFile(File file) {
		logger.debug("Reading file: "+file.getName());
		termProbs = new HashMap<String, Double>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String[] termWeight = scanner.nextLine().split("\t");
				termProbs.put(termWeight[0].trim(), Double.parseDouble(termWeight[1]));
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public Map<String, Double> getTermProbs() {
		return termProbs;
	}

}
