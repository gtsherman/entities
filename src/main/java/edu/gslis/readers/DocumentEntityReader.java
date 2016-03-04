package edu.gslis.readers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentEntityReader extends AbstractReader {
	
	static final Logger logger = LoggerFactory.getLogger(DocumentEntityReader.class);

	private Map<String, Map<String, Double>> documentEntities;
	
	@Override
	public void readFile(File file) {
		documentEntities = new HashMap<String, Map<String, Double>>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				try {
					String[] parts = line.split("\t");
					String document = parts[0];
					String entity = parts[1].trim();
					double confidence = Double.parseDouble(parts[2].trim());
					if (!documentEntities.containsKey(document)) {
						documentEntities.put(document, new HashMap<String, Double>());
					}
					documentEntities.get(document).put(entity, confidence);
				} catch (ArrayIndexOutOfBoundsException e) {
					logger.error("Error reading line: "+line);
				}
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public List<String> getEntities(String document) {
		if (documentEntities.containsKey(document))
			return new ArrayList<String>(documentEntities.get(document).keySet());
		logger.warn("No document "+document+" recorded");
		return new ArrayList<String>();
	}
	
	public Set<String> getAllEntities() {
		Set<String> entities = new HashSet<String>();
		Iterator<String> it = documentEntities.keySet().iterator();
		while (it.hasNext()) {
			Set<String> docEnts = documentEntities.get(it.next()).keySet();
			entities.addAll(docEnts);
		}
		return entities;
	}
	
	public Set<String> getDocuments() {
		return documentEntities.keySet();
	}
	
	public double getEntityConfidence(String document, String entity) {
		if (!documentEntities.containsKey(document) || !documentEntities.get(document).containsKey(entity)) {
			return 0.0;
		}
		return documentEntities.get(document).get(entity);
	}
}
