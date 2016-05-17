package edu.gslis.main;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.validators.DoubleEntityValidator;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queryrunning.DoubleEntityRunner;
import edu.gslis.readers.QueryProbabilityReader;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunDoubleEntityValidation {
	
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
		
		CollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(config.get("index"));
		
		DoubleEntityRunner runner = new DoubleEntityRunner(index, qpreader, cs, stopper);

		DoubleEntityValidator validator = new DoubleEntityValidator(runner, 5);
		validator.setQueries(queries);
		validator.setQrels(qrels);
		
		double map = validator.evaluate(new MAPEvaluator());
		System.out.println("Average MAP: "+map);
	}

}
