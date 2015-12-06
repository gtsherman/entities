package edu.gslis.entities.readers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PrecomputedCategoryProbabilityReader extends AbstractReader {

	Map<String, Map<Integer, Map<String, Double>>> probabilities;
	
	@Override
	public void readFile(File file) {
		// <Query, <DocID, <Term, Score>>>
		Map<String, Map<Integer, Map<String, Double>>> probabilities = new HashMap<String, Map<Integer, Map<String, Double>>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				String[] parts = line.split("\\|");

				String query = parts[0];
				int docId = Integer.parseInt(parts[2]);
				String term = parts[3];
				double score = Double.parseDouble(parts[4]);
				
				if (probabilities.get(query) == null) {
					probabilities.put(query, new HashMap<Integer, Map<String, Double>>());
				}
				if (probabilities.get(query).get(docId) == null) {
					probabilities.get(query).put(docId, new HashMap<String, Double>());
				}
				probabilities.get(query).get(docId).put(term, score);
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println("Error reading precomputed stats file");
			e.printStackTrace();
		}
	}
	
	public Map<String, Map<Integer, Map<String, Double>>> getProbabilities() {
		return probabilities;
	}

}
