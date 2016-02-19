package edu.gslis.entities.readers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RetrievedDocsReader extends AbstractReader implements Reader {
	private String thisClass = "[RetrievedDocsReader] ";

	private Map<String, List<String>> retrievedDocs;
	
	@Override
	public void readFile(File file) {
		retrievedDocs = new HashMap<String, List<String>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				try {
					String[] parts = line.split(" ");
					String query = parts[0];
					String doc = parts[2];
					
					if (!retrievedDocs.containsKey(query)) {
						retrievedDocs.put(query, new ArrayList<String>());
					}
					retrievedDocs.get(query).add(doc);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println(thisClass+"Error reading line: "+line);
				}
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.err.println(thisClass+retrievedDocs.keySet().size()+" queries.");
	}
	
	public List<String> getDocsForQuery(String query) {
		return retrievedDocs.get(query);
	}
	
}
