package edu.gslis.docscoring.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.gslis.readers.ModelReader;
import edu.gslis.readers.TSVReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.utils.NameToFileNameConverter;

public class CategoryProbability {

	public double EPSILON = 0.001;

	private String modelDir;
	private String docEntitiesDir;
	private Map<String, List<String>> entityCategories;
	private Map<String, List<String>> categoryEntities;
	private SearchHit doc;
	
	/**
	 * Constructor method.
	 * @param modelRootDir	The root directory containing the category models.
	 * @param entitiesRootDir	The root directory containing the per-document entities.
	 */
	public CategoryProbability(String modelRootDir, String entitiesRootDir, String entityCategoriesFile) {
		this.setModelRootDir(modelRootDir);
		this.setDocEntitiesRootDir(entitiesRootDir);
		this.readEntityCategoriesFile(entityCategoriesFile);
	}
	
	public void setDoc(SearchHit doc) {
		this.doc = doc;
	}
	
	public void setModelRootDir(String dir) {
		this.modelDir = dir;
	}
	
	public void readEntityCategoriesFile(String file) {
		this.entityCategories = new HashMap<String, List<String>>();
		try {
			Scanner scanner = new Scanner(new File(file));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("\\|");

				String entity = parts[0];
				List<String> categories = new ArrayList<String>();
				for (int i = 1; i < parts.length; i++) {
					String category = parts[i].trim();
					categories.add(category);
				}
				this.entityCategories.put(entity, categories);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("Unable to find categories file: "+file);
			System.exit(-1);
		}
		
		this.categoryEntities = new HashMap<String, List<String>>();
		for (String entity : this.entityCategories.keySet()) {
			this.categoryEntities.put(entity, new ArrayList<String>());
			for (String category : this.entityCategories.get(entity)) {
				this.categoryEntities.get(entity).add(category);
			}
		}
	}
	
	public void setDocEntitiesRootDir(String dir) {
		this.docEntitiesDir = dir;
	}
	
	public Map<String, Double> getProbability(List<String> terms) {
		Set<String> categories = this.getDocumentCategories();
		NameToFileNameConverter nc; 
		ModelReader reader = new ModelReader();
		
		Map<String, List<Double>> termProbs = new HashMap<String, List<Double>>();
		for (String term : terms) {
			termProbs.put(term, new ArrayList<Double>());
		}

		for (String category : categories) {
			try {
				nc = new NameToFileNameConverter(category);
				File modelFile = new File(this.modelDir+"/"+nc.getFirstChar()+"/"+nc.getSecondChar()+"/"+category);
				Map<String, Double> termScores = reader.readFile(modelFile);

				for (String term : terms) {
					if (termScores.containsKey(term)) {
						termProbs.get(term).add(termScores.get(term));
						//categoryProbs.add(termScores.get(term));
					} else {
						termProbs.get(term).add(this.EPSILON);
						//categoryProbs.add(this.EPSILON);
					}
				}
			} catch (Exception e) {
				System.err.println("Error reading probabilities for category: "+category);
				System.err.println("\tFor document: "+this.doc.getDocno());
				System.err.println("\tFor entities: ");
				for (String cat : this.getDocumentEntitiesForCategory(category)) {
					System.err.println("\t\t"+cat);
				}
				e.printStackTrace();
			}
		}

		Map<String, Double> returnProbs = new HashMap<String, Double>();
		for (String term : termProbs.keySet()) {
			double prob = this.EPSILON;

			// Sort category probs in descending order
			List<Double> categoryProbs = termProbs.get(term);
			Collections.sort(categoryProbs);
			Collections.reverse(categoryProbs);

			if (categoryProbs.size() > 0) {
				System.err.print("\t\t\tProbabilities for term "+term+": ");
				//for (double p : categoryProbs) {
				for (int i = 0; i < 3; i++) {
					double p = categoryProbs.get(i);
					System.err.print(p+", ");
				}
				System.err.println();
				prob = categoryProbs.get(0);
				//for (int i = 1; i < categoryProbs.size(); i++) {
				for (int i = 1; i < 3; i++) {
					prob *= categoryProbs.get(i);
				}
			}
			returnProbs.put(term, prob);
		}
		return returnProbs;
	}
	
	private Set<String> getDocEntities() {
		int entityIndex = 6;

		Set<String> entities = new HashSet<String>();
		try {
			File entityFile = new File(this.docEntitiesDir+"/"+this.doc.getDocno()+".tsv");
			if (entityFile.exists()) {
				TSVReader reader = new TSVReader();
				List<List<String>> lines = reader.readFile(entityFile);
				for (List<String> line : lines) {
					String entity = line.get(entityIndex);
					entities.add(entity);
				}
			}
		} catch (Exception e) {
			System.err.println("Error gathering entities from file: "+this.doc.getDocno());
			System.err.println(this.docEntitiesDir+"/"+this.doc.getDocno()+".tsv");
		}
		return entities;
	}
	
	private Set<String> getDocumentCategories() {
		Set<String> entities = this.getDocEntities();
		Set<String> categories = new HashSet<String>();
		for (String entity : entities) {
			if (this.entityCategories.containsKey(entity)) {
				categories.addAll(this.entityCategories.get(entity));
			} else {
				System.err.println("Why are we missing this entity? "+entity);
			}
		}
		return categories;
	}
	
	private Set<String> getDocumentEntitiesForCategory(String category) {
		Set<String> entities = this.getDocEntities();
		Set<String> categories = new HashSet<String>();
		for (String entity : entities) {
			if (this.entityCategories.containsKey(entity) && this.entityCategories.get(entity).contains(category)) {
				categories.add(entity);
			}
		}
		return categories;
	}
}