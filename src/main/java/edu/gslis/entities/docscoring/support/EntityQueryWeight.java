package edu.gslis.entities.docscoring.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.gslis.entities.readers.DocumentEntityReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class EntityQueryWeight {

	private String thisClass = "[EntityQueryWeight] ";

	private SearchHits hits;
	private DocumentEntityReader de;
	
	private Map<String, Double> scores;
	private double total;
	
	private final static int DEFAULT_K = 50;
	
	public EntityQueryWeight(SearchHits hits, DocumentEntityReader de) {
		this(hits, de, DEFAULT_K);
	}
	
	public EntityQueryWeight(SearchHits hits, DocumentEntityReader de, int k) {
		this.hits = hits;
		this.de = de;
		
		score(k);
	}
	
	private void score(int k) {
		scores = new HashMap<String, Double>();
		
		Iterator<SearchHit> hiterator = hits.iterator();
		while (hiterator.hasNext() && k >= 0) {
			SearchHit hit = hiterator.next();
			k--;
			
			String docno = hit.getDocno();
			List<String> entities = de.getEntities(docno);
			
			for (String entity : entities) {
				if (!scores.containsKey(entity)) {
					scores.put(entity, 0.0);
				}
				scores.put(entity, scores.get(entity)+1);
				total += 1;
			}
		}
		
		for (String entity : scores.keySet()) {
			scores.put(entity, scores.get(entity) / total);
		}
	}
	
	public double getScore(String entity) {
		if (scores.containsKey(entity)) {
			return scores.get(entity);
		}
		return 1.0/de.getAllEntities().size();
	}
	
}
