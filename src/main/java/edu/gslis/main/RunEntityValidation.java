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
import edu.gslis.evaluation.running.runners.EntityRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.factory.DataSourceFactory;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DataSource;

public class RunEntityValidation {
	
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
		
		String model = EntityRunner.WIKI_MODEL;
		if (config.get("entity-model") != null) {
			String entityModel = config.get("entity-model");
			if (entityModel.equalsIgnoreCase(EntityRunner.SELF_MODEL)) {
				model = EntityRunner.SELF_MODEL;
			}
		}
		
		DataSource data = DataSourceFactory.getDataSource(config.get("intial-hits"),
				config.get("database"), SearchResultsDataInterpreter.DATA_NAME);
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);
		
		long seed = Long.parseLong(args[1]);
		
		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}

		FileLookupDocScorer docScorer = new FileLookupDocScorer(forQueryProbs + 
				File.separator + "docProbsNew");
		FileLookupDocScorer expansionDocScorer = new FileLookupDocScorer(forQueryProbs + 
				File.separator + "entityProbs" + model + "New.10");
		
		EntityRunner runner = new EntityRunner(initialHitsBatch, stopper, docScorer, expansionDocScorer);
		KFoldValidator validator = new KFoldValidator(runner, 10);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("singleEntity", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
