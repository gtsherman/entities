package edu.gslis.entities.readers;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class TopEntitiesReader extends AbstractReader implements Reader {
	private String thisClass = "[TopEntitiesReader] ";

	private Map<String, Set<String>> queryEntities;
	
	@Override
	public void readFile(File file) {
		queryEntities = new HashMap<String, Set<String>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				try {
					String[] parts = line.split("\t");
					String query = parts[0];
					String entity = parts[1].trim();
					if (!queryEntities.containsKey(query)) {
						queryEntities.put(query, new HashSet<String>());
					}
					queryEntities.get(query).add(entity);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println(thisClass+"Error reading line: "+line);
				}
			}	
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public Set<String> getEntities(String query) {
		if (!queryEntities.containsKey(query)) {
			return new HashSet<String>();
		}
		return queryEntities.get(query);
	}
}
