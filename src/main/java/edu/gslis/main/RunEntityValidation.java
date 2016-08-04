package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.validators.EntityValidator;
import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queryrunning.EntityRunner;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunEntityValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String forQueryProbs = config.get("for-query-probs");
		
		QueryProbabilityReader qpreader = new QueryProbabilityReader();
		qpreader.setBasePath(forQueryProbs);
		
		EntityRunner runner = new EntityRunner(index, qpreader, stopper);

		EntityValidator validator = new EntityValidator(runner, 10);
		validator.setQueries(queries);
		validator.setQrels(qrels);
		
		long seed = Long.parseLong(args[1]);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, new MAPEvaluator());
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
