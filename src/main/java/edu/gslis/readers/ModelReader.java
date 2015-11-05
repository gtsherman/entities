package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ModelReader {
	
	public Map<String, Double> readFile(String fileName) {
		File file = new File(fileName);
		return this.readFile(file);
	}
	
	public Map<String, Double> readFile(File file) {
		Map<String, Double> termScores = new HashMap<String, Double>();
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
			System.err.println("Couldn't find model file: "+file.getName());
			System.err.println(file.getAbsolutePath());
		}
		return termScores;
	}
	
}
