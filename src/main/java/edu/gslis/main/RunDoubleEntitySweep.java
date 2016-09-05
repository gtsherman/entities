package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunDoubleEntitySweep {
	
	static final Logger logger = LoggerFactory.getLogger(RunDoubleEntitySweep.class);

	public static void main(String[] args) throws IOException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = null;
		if (config.get("stoplist") != null)
			stopper = new Stopper(config.get("stoplist"));
		
		GQueriesJsonImpl queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		
		QueryProbabilityReader qpreader = new QueryProbabilityReader();
		qpreader.setBasePath(config.get("entity-probs"));

		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		int numDocs = 1000;
		if (config.get("num-docs") != null) {
			numDocs = Integer.parseInt(config.get("num-docs"));
		}
		
		String outDir = config.get("out-dir"); 

		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		QueryRunner runner = new DoubleEntityRunner(index, qpreader, stopper);
		
		Map<String, Double> params = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			params.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;

				String run = origWeight+"_"+wikiWeight+"_"+selfWeight;
				output.setRunId(run);
				output.setWriter(new FileWriter(outDir+"/"+run));
				
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
