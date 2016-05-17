package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import edu.gslis.textrepresentation.FeatureVector;

public class RelevanceModelReader extends AbstractReader {

	private FeatureVector model;
	
	public void readFile(File file) {
		model = new FeatureVector(null);
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String[] weightTerm = scanner.nextLine().split(" ");
				model.setTerm(weightTerm[1].trim(), Double.parseDouble(weightTerm[0]));
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public FeatureVector getVector() {
		return model;
	}

}
