package edu.gslis.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class QueryDocs extends AbstractReader {

	Map<String, SearchHits> queryDocs;

	public void readFile(File file) {
		queryDocs = new HashMap<String, SearchHits>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String[] parts = scanner.nextLine().split(" ");
				String query = parts[0];
				String document = parts[1].trim();
				
				if (!queryDocs.containsKey(query)) {
					queryDocs.put(query, new SearchHits());
				}
				
				SearchHit doc = new SearchHit();
				doc.setDocno(document);
				queryDocs.get(query).add(doc);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public SearchHits getDocsForQuery(String query) {
		if (queryDocs.containsKey(query)) {
			return queryDocs.get(query);
		}
		return null;
	}
	
	public SearchHits getDocsForQuery(GQuery query) {
		return getDocsForQuery(query.getTitle());
	}
}
