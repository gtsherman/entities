package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.QueryProbDatabaseLookupDocScorer;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;

public class RunDoubleEntitySweep {
	
	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));
		
		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		String outDir = config.get("double-entity-sweep-dir"); 

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("doubleEntity", outputWriter);
		
		DatabaseDataSource expansionDataWiki = new DatabaseDataSource(
				dbCon, "expansion_probabilities_wiki");
		DatabaseDataSource expansionDataSelf = new DatabaseDataSource(
				dbCon, "expansion_probabilities_self");

		DocScorer docScorer = new CachedDocScorer(new FileLookupDocScorer(
				config.get("document-probability-data-dir")));
		DocScorer expansionDocScorerWiki = new CachedDocScorer(new QueryProbDatabaseLookupDocScorer(expansionDataWiki,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));
		DocScorer expansionDocScorerSelf = new CachedDocScorer(new QueryProbDatabaseLookupDocScorer(expansionDataSelf,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));


		QueryRunner runner = new DoubleEntityRunner(initialHitsBatch, stopper,
				docScorer, expansionDocScorerWiki, expansionDocScorerSelf);
		
		Map<String, Double> params = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			params.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;

				String run = origWeight + "_" + wikiWeight + "_" + selfWeight;
				output.setRunId(run);
				output.setWriter(new FileWriter(outDir + File.separator + run));
				
				params.put(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
				params.put(DoubleEntityRunner.SELF_WEIGHT, selfWeight);

				SearchHitsBatch batchResults = runner.run(queries, numDocs, params);
			
				Iterator<String> qit = batchResults.queryIterator();
				while (qit.hasNext()) {
					String query = qit.next();
					SearchHits hits = batchResults.getSearchHits(query);
					output.write(hits, query);
				}
			}
		}
		output.close();
	}

}
