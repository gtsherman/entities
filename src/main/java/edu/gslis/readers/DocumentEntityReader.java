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

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class DocumentEntityReader extends AbstractReader {
	
	private int limit = 10;
	
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
					if (documentEntities.get(document).keySet().size() >= limit) {
						continue;
					}
					documentEntities.get(document).put(entity, confidence);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println("Error reading line: "+line);
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
		System.err.println("No document "+document+" recorded");
		return new ArrayList<String>();
	}
	
	public SearchHits getEntitiesAsSearchHits(String document, IndexWrapper index) {
		if (documentEntities.containsKey(document)) {
			SearchHits hits = new SearchHits();

			Set<String> entities = documentEntities.get(document).keySet();
			for (String entity : entities) {
				SearchHit hit = new SearchHit();
				hit.setDocno(entity);
				hit.setDocID(index.getDocId(entity));
				hit.setScore(documentEntities.get(document).get(entity));
				hit.setQueryName(document);
				hits.add(hit);
			}
			
			return hits;
		}
		System.err.println("No document "+document+" recorded");
		return new SearchHits();
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
	
	public void setLimit(int numEntities) {
		this.limit = numEntities;
	}
}
