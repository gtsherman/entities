package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;

public class RunRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String rmsDir = config.get("rms-dir");
		String targetMetric = config.get("target-metric");
		
		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}
		
		long seed = Long.parseLong(args[1]);
		
		QueryRunner runner = new RMRunner(index, stopper, rmsDir);
		KFoldValidator validator = new KFoldValidator(runner);

		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
