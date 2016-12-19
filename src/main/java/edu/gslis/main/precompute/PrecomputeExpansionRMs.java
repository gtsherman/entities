package edu.gslis.main.precompute;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.StandardRM1Builder;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class PrecomputeExpansionRMs {
	
	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = null;
		if (config.get("wiki-index") != null)
			wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		int numEntities = 10;
		if (config.get("num-entities") != null) {
			numEntities = Integer.parseInt(config.get("num-entities"));
		}
		
		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();
		
		Set<SearchHit> all = new HashSet<SearchHit>();
		Iterator<SearchHits> hitsIt = initialHitsBatch.searchHitIterator();
		while (hitsIt.hasNext()) {
			for (SearchHit hit : hitsIt.next()) {
				if (all.contains(hit)) {
					continue;
				}

				// Convert to query
				GQuery query = new GQuery();
				query.setFeatureVector(hit.getFeatureVector());
				query.applyStopper(stopper);
				query.getFeatureVector().clip(20);
				
				RM1Builder rm1 = new StandardRM1Builder(query, wikiIndex, numEntities, 20);
				FeatureVector rmVec = rm1.buildRelevanceModel(stopper);
				
				System.out.println(rmVec.toString(10));
				
				all.add(hit);
			}
		}
	}
	
}
