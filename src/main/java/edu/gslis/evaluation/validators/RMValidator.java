package edu.gslis.evaluation.validators;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;
import edu.gslis.queryrunning.QueryRunner;
import edu.gslis.queryrunning.RMRunner;

public class RMValidator extends KFoldValidator {

	public RMValidator(QueryRunner runner) {
		super(runner);
	}
	
	public RMValidator(QueryRunner runner, int k) {
		super(runner, k);
	}
	
	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		runner.setQueries(queries);
		
		double maxMetric = 0.0;
		Map<String, Double> bestParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			
			runner.setParameter(RMRunner.ORIG_QUERY_WEIGHT, origWeight);
			runner.run(1000);
			
			double metricVal = evaluator.evaluate(runner.getBatchResults(), qrels);
			if (metricVal > maxMetric) {
				maxMetric = metricVal;
				bestParams.put(RMRunner.ORIG_QUERY_WEIGHT, origWeight);
			}
		}
		
		return bestParams;
	}

}
