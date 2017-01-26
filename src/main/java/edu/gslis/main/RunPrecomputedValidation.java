package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.cli.Option;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.PrecomputedRunner;
import edu.gslis.evaluation.running.runners.support.PrecomputedSearchResults;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.config.CommandLineConfiguration;
import edu.gslis.utils.config.Configuration;

public class RunPrecomputedValidation {

	public static void main(String[] args) {
		CommandLineConfiguration config = new CommandLineConfiguration(
				new Option("d", "sweep-directory", true, "sweep directory"),
				new Option("m", "target-metric", true, "target metric"),
				new Option("r", "seed", true, "random seed"));
		config.read(args);
		
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get(Configuration.QUERIES_PATH));

		Qrels qrels = new Qrels(
				config.get(Configuration.QRELS_PATH),
				false, 1);
		
		Evaluator evaluator = new MAPEvaluator(qrels);
		if (config.get("m") != null &&
				config.get("m").equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}

		String sweepDir = config.get("d");
		File directory = new File(sweepDir);
		if (!directory.isDirectory()) {
			System.err.println("Not a directory: " + sweepDir);
			System.exit(-1);
		}
		
		long seed = 1;
		if (config.get("r") != null) {
			seed = Long.parseLong(config.get("r"));
		}

		PrecomputedSearchResults precomputedResults = new PrecomputedSearchResults();
		precomputedResults.addResults(directory.listFiles());

		KFoldValidator validator = new KFoldValidator(new PrecomputedRunner(precomputedResults));
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
	
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("precomputed", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
