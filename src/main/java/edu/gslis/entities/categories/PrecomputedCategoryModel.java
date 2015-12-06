package edu.gslis.entities.categories;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.entities.readers.AbstractReader;

public class PrecomputedCategoryModel extends AbstractReader implements CategoryModel {
	
	private static String thisClass = "[PrecomputedCategoryModel] ";
	
	public Map<String, Double> termScores;

	@Override
	public void readFile(File file) {
		termScores = new HashMap<String, Double>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length() <= 1) {
					continue;
				}
				String[] parts = line.split(" ");

				double score = Double.parseDouble(parts[0]);
				String term = parts[1].trim();
				
				termScores.put(term, score);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println(thisClass+"Couldn't find model file: "+file.getAbsolutePath());
		}
	}
	
	public double getScore(String term) {
		Double score = this.termScores.get(term);
		if (score == null) {
			return 0.0;
		}
		return score;
	}

}
