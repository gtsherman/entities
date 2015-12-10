package edu.gslis.entities.readers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DocumentEntityReader extends AbstractReader {

	private Map<String, List<String>> documentEntities;
	
	@Override
	public void readFile(File file) {
		documentEntities = new HashMap<String, List<String>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				String[] parts = line.split("\t");
				String document = parts[0];
				String entity = parts[1].trim();
				if (!documentEntities.containsKey(document)) {
					documentEntities.put(document, new ArrayList<String>());
				}
				documentEntities.get(document).add(entity);
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public List<String> getEntities(String document) {
		if (documentEntities.containsKey(document))
			return documentEntities.get(document);
		return new ArrayList<String>();
	}
}
