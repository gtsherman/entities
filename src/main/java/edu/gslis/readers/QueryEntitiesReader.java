package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class QueryEntitiesReader extends AbstractReader {

	Map<String, SearchHits> queryEntities;
	
	@Override
	public void readFile(File file) {
		queryEntities = new HashMap<String, SearchHits>();
		Map<String, Integer> queryTotals = new HashMap<String, Integer>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String[] parts = scanner.nextLine().split("\t");

				String query = parts[0];
				String entity = parts[2];
				int freq = Integer.parseInt(parts[3]);
				
				if (!queryEntities.containsKey(query)) {
					queryEntities.put(query, new SearchHits());
				}
				if (!queryTotals.containsKey(query)) {
					queryTotals.put(query, 0);
				}
				
				SearchHit hit = new SearchHit();
				hit.setDocno(entity);
				hit.setScore(freq);

				queryEntities.get(query).add(hit);;
				queryTotals.put(query, queryTotals.get(query)+freq);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		for (String query : queryEntities.keySet()) {
			SearchHits hits = queryEntities.get(query);
			Iterator<SearchHit> hitit = hits.iterator();
			while (hitit.hasNext()) {
				SearchHit hit = hitit.next();
				hit.setScore(Math.log(hit.getScore() / (double) queryTotals.get(query)));
			}
		}
	}
	
	public SearchHits getEntitiesForQuery(String query) {
		if (!queryEntities.containsKey(query)) {
			return null;
		}
		return queryEntities.get(query);
	}
	
	public SearchHits getEntitiesForQuery(GQuery query) {
		return getEntitiesForQuery(query.getTitle());
	}
	
}
