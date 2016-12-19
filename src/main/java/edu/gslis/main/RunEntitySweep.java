package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunEntitySweep {
	
	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();
		
		String entityProbsPath = config.get("for-query-probs");
		String outDir = config.get("single-entity-sweep-dir"); 
		
		String entityModel = config.get("entity-model");

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("singleEntity", outputWriter);

		FileLookupDocScorer docScorer = new FileLookupDocScorer(entityProbsPath + 
				File.separator + "docProbsNew");
		FileLookupDocScorer expansionDocScorer = new FileLookupDocScorer(entityProbsPath + 
				File.separator + "entityProbs" + entityModel + "New.10");
		
		EntityRunner runner = new EntityRunner(initialHitsBatch, stopper, docScorer, expansionDocScorer);
		
		Map<String, Double> params = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			params.put(EntityRunner.DOCUMENT_WEIGHT, origWeight);
			
			double wikiWeight = (10 - origW) / 10.0;
			params.put(EntityRunner.EXPANSION_WEIGHT, wikiWeight);
				
			String run = origWeight + "_" + wikiWeight;
			output.setRunId(run);
			output.setWriter(new FileWriter(outDir + File.separator + run));
				
			SearchHitsBatch batchResults = runner.run(queries, numDocs, params);
			
			Iterator<String> qit = batchResults.queryIterator();
			while (qit.hasNext()) {
				String query = qit.next();
				SearchHits hits = batchResults.getSearchHits(query);
				output.write(hits, query);
			}
		}
	
		output.close();
	}

}
