package edu.gslis.evaluation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.queryrunning.QueryRunner;

public abstract class KFoldValidator implements Validator {
	
	protected Qrels qrels;
	protected GQueries queries;
	protected int k;
	protected int numResults = 1000;
	
	protected QueryRunner runner;
	protected Evaluator evaluator;
	
	public KFoldValidator(QueryRunner runner) {
		this(runner, 10);
	}
	
	public KFoldValidator(QueryRunner runner, int k) {
		this.runner = runner;
		this.k = k;
	}

	public void setQrels(Qrels qrels) {
		this.qrels = qrels;
	}

	public void setQueries(GQueries queries) {
		this.queries = queries;
	}
	
	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}
	
	public void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}
	
	public SearchHitsBatch evaluate(Evaluator evaluator) {
		Random r = new Random();
		return evaluate(r.nextLong(), evaluator);
	}

	public SearchHitsBatch evaluate(long seed, Evaluator evaluator) {
		List<GQuery> queryList = new ArrayList<GQuery>();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext())
			queryList.add(queryIt.next());

		// Partition the dataset
		Collections.shuffle(queryList, new Random(seed));
		List<List<GQuery>> queryChunks = new ArrayList<List<GQuery>>();
		for (int i = 0; i < queryList.size(); i++) {
			int c = i % k; // the chunk to use

			if (queryChunks.size() <= c)
				queryChunks.add(c, new ArrayList<GQuery>());

			queryChunks.get(c).add(queryList.get(i));
		}
		
		System.err.println("Split into "+queryChunks.size()+" chunks.");
		
		SearchHitsBatch batchResults = new SearchHitsBatch();
		
		// Run the evaluation
		for (int t = 0; t < k; t++) { // t is the test chunk
			System.err.println("Running fold "+(t+1)+"/"+k);
			
			// Set up training queries
			GQueries trainingQueries = new GQueriesJsonImpl();
			for (int i = 0; i < k; i++) {
				if (i == t)
					continue;
				
				for (GQuery query : queryChunks.get(i))
					trainingQueries.addQuery(query);
			}
			System.err.println("\tAdded "+trainingQueries.numQueries()+" training queries");
			
			// Train
			System.err.println("\tTraining...");
			Map<String, Double> parameters = sweep(trainingQueries, evaluator);

			// Set up testing queries
			GQueries testingQueries = new GQueriesJsonImpl();
			for (GQuery query : queryChunks.get(t))
				testingQueries.addQuery(query);
			runner.setQueries(testingQueries);
			for (String param : parameters.keySet())
				runner.setParameter(param, parameters.get(param));

			// Test
			System.err.println("\tTesting...");
			runner.run(numResults);
			batchResults.addBatchResults(runner.getBatchResults());
		}
		
		return batchResults;
	}
	
	public abstract Map<String, Double> sweep(GQueries queries, Evaluator evaluator);

}
