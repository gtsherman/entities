package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RM3Builder;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunBaselineRelevanceModel {
	
	public static void main(String[] args) {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));

		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		SearchResultsReader resultsReader = new SearchResultsReader(new File(config.get("initial-hits")), index);
		SearchHitsBatch batchResults = resultsReader.getBatchResults();
		
		int fbDocs = 20;
		if (config.get("fb-docs") != null) {
			fbDocs = Integer.parseInt(config.get("fb-docs"));
		}
		
		int fbTerms = 20;
		if (config.get("fb-terms") != null) {
			fbTerms = Integer.parseInt(config.get("fb-terms"));
		}
		
 		double origQueryWeight = 0.5;
 		if (config.get("original-query-weight") != null) {
 			origQueryWeight = Double.parseDouble(config.get("original-query-weight"));
 		}
 		if (args.length > 1) {
 			origQueryWeight = Double.parseDouble(args[1]);
 		}
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("rm3", outputWriter);
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			SearchHits hits = batchResults.getSearchHits(query);

			RM1Builder rmBuilder = new RM1Builder(query, hits, fbDocs, fbTerms, cs);
			RM3Builder rm3Builder = new RM3Builder(query, rmBuilder);
			FeatureVector rmVec = rm3Builder.buildRelevanceModel(origQueryWeight, stopper);
			
			System.err.println(rmVec.toString(10));
			
			GQuery rmQuery = new GQuery();
			rmQuery.setFeatureVector(rmVec);
			rmQuery.setTitle(query.getTitle());
			
			hits = index.runQuery(rmQuery, numDocs);
			output.write(hits, query.getTitle());
		}
		output.close();
	}

}
