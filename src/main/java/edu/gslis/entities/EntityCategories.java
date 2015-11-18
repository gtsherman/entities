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
	
	private Map<String, Set<String>> entityCategories;
	private Map<String, Set<String>> categoryEntities;

	@Override
	public void readFile(File file) {
		entityCategories = new HashMap<String, Set<String>>();
		categoryEntities = new HashMap<String, Set<String>>();

		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("|");

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
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find file: "+file.getName());
		}
	}
	
	public Set<String> getCategories(String entity) {
		return this.entityCategories.get(entity);
	}
	
	public Set<String> getEntities(String category) {
		return this.categoryEntities.get(category);
	}

}
