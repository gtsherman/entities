package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.DoubleEntityRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.scoring.DocScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.readers.SearchResultsReader;

public class RunDoubleEntityValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapper index = new IndexWrapperIndriImpl(config.get("index"));
		
		Stopper stopper = new Stopper(config.get("stoplist"));

		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));

		Qrels qrels = new Qrels(config.get("qrels"), false, 1);

		String forQueryProbs = config.get("for-query-probs");
		String targetMetric = config.get("target-metric");
		
		int numEntities = 10;
		if (config.get("num-entities") != null) {
			numEntities = Integer.parseInt(config.get("num-entities"));
		}
		if (args.length > 2) {
			numEntities = Integer.parseInt(args[2]);
		}

		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}
		
		SearchHitsBatch initialHitsBatch = (new SearchResultsReader(new File(config.get("initial-hits")), index)).getBatchResults();
		
		long seed = Long.parseLong(args[1]);
		
		DocScorer docScorer = new FileLookupDocScorer(forQueryProbs + 
				File.separator + "docProbsNew");
		DocScorer wikiDocScorer = new FileLookupDocScorer(forQueryProbs + 
				File.separator + "entityProbsWikiNew." + numEntities);
		DocScorer selfDocScorer = new FileLookupDocScorer(forQueryProbs + 
				File.separator + "entityProbsSelfNew." + numEntities);

		DoubleEntityRunner runner = new DoubleEntityRunner(initialHitsBatch, stopper,
				docScorer, selfDocScorer, wikiDocScorer);
		KFoldValidator validator = new KFoldValidator(runner, 10);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("doubleExpansion", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
