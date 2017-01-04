package edu.gslis.main.precompute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.StandardRM1Builder;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;
import edu.gslis.utils.retrieval.QueryResults;

public class CreateRMs {

	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		CollectionStats collectionStats = new IndexBackedCollectionStats();
		collectionStats.setStatSource(config.get("index"));
		
		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		String outDir = config.get("rms-dir");
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}
		
		SearchResultsReader resultsReader = new SearchResultsReader(new File(config.get("initial-hits")), index);
		SearchHitsBatch batchResults = resultsReader.getBatchResults();
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);
			
			System.err.println("Query "+query.getTitle());

			if (query.getFeatureVector().getLength() == 0) {
				continue;
			}
			
			QueryResults queryResults = new QueryResults(query, batchResults.getSearchHits(query));
			
			// RM1 built on target index
			RM1Builder rm1 = new StandardRM1Builder(fbDocs, fbTerms, collectionStats);
			FeatureVector rmVec = rm1.buildRelevanceModel(queryResults, stopper);
			rmVec.normalize();
			
			FileWriter out = new FileWriter(outDir + File.separator + query.getTitle());
			out.write(rmVec.toString(50));
			out.close();
		}
	}

}
