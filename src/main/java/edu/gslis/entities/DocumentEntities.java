package edu.gslis.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.gslis.entities.readers.AbstractReader;

public class DocumentEntities extends AbstractReader {
	
	private static String thisClass = "[DocumentEntities] ";

	private Map<String, Integer> entityCount;
	
	@Override
	public void readFile(File file) {
		System.err.println(thisClass+"Reading file "+file.getAbsolutePath());
		entityCount = new HashMap<String, Integer>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length() < 1) {
					continue;
				}
				String[] parts = line.split("\\t");

				String entity = parts[parts.length-1];
				if (!entityCount.containsKey(entity))
					entityCount.put(entity, 0);
				int count = entityCount.get(entity);
				entityCount.put(entity, count+1);
			}
			scanner.close();
			System.err.println(thisClass+"Found TSV file: "+file.getName());
		} catch (FileNotFoundException e) {
			System.err.println(thisClass+"Couldn't find TSV file: "+file.getName());
		}
	}
	
	public Set<String> getEntities() {
		return this.entityCount.keySet();
	}
	
	public int getEntityFreq(String entity) {
		if (!this.entityCount.containsKey(entity))
			return 0;
		return this.entityCount.get(entity);
	}
	
	public int getNumberOfEntities() {
		int sum = 0;
		for (String entity : this.entityCount.keySet()) {
			sum += this.entityCount.get(entity);
		}
		return sum;
	}
	
	public int getNumberOfUniqueEntities() {
		return this.entityCount.keySet().size();
	}

}
