package edu.gslis.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.gslis.entities.readers.AbstractReader;

public class EntityCategories extends AbstractReader {
	
	private static String thisClass = "[EntityCategories] ";
	
	private Map<String, Set<String>> entityCategories;
	private Map<String, Set<String>> categoryEntities;

	@Override
	public void readFile(File file) {
		System.err.println(thisClass+"Reading entity categories file: "+file.getAbsolutePath());
		entityCategories = new HashMap<String, Set<String>>();
		categoryEntities = new HashMap<String, Set<String>>();

		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("\\|");

				String entity = parts[0];
				entityCategories.put(entity, new HashSet<String>());

				String category;
				for (int i = 1; i < parts.length; i++) {
					category = parts[i].trim();
					entityCategories.get(entity).add(category);
					if (categoryEntities.get(category) == null) {
						categoryEntities.put(category, new HashSet<String>());
					}
					categoryEntities.get(category).add(entity);
				}
			}
			scanner.close();
			System.err.println(thisClass+"Read "+entityCategories.keySet().size()+" entities");
		} catch (FileNotFoundException e) {
			System.err.println(thisClass+"Couldn't find file: "+file.getName());
		}
	}
	
	public Set<String> getCategories(String entity) {
		Set<String> categories = this.entityCategories.get(entity);
		if (categories == null) {
			return new HashSet<String>();
		}
		return categories;
	}
	
	public Set<String> getEntities(String category) {
		Set<String> entities = this.categoryEntities.get(category);
		if (entities == null) {
			return new HashSet<String>();
		}
		return entities;
	}

}
