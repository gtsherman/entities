package edu.gslis.evaluation.validators;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;
import edu.gslis.queryrunning.QueryRunner;
import edu.gslis.queryrunning.SecondOrderRMRunner;

public class SecondOrderRMValidator extends KFoldValidator {

	public SecondOrderRMValidator(QueryRunner runner) {
		super(runner);
	}
	
	public SecondOrderRMValidator(QueryRunner runner, int k) {
		super(runner, k);
	}

	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		runner.setQueries(queries);
		
		double maxMetric = 0.0;
		Map<String, Double> bestParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			runner.setParameter(SecondOrderRMRunner.ORIG_QUERY_WEIGHT, origWeight);
			for (int targetRMW = 0; targetRMW <= 10-origW; targetRMW++) {
				double targetRMWeight = targetRMW / 10.0;
				double wikiRMWeight = (10-(origW+targetRMW)) / 10.0;
				
				runner.setParameter(SecondOrderRMRunner.TARGET_RM_WEIGHT, targetRMWeight);
				runner.setParameter(SecondOrderRMRunner.EXTERNAL_RM_WEIGHT, wikiRMWeight);

				runner.run(100);
				
				double metricVal = evaluator.evaluate(runner.getBatchResults(), qrels);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.put(SecondOrderRMRunner.ORIG_QUERY_WEIGHT, origWeight);
					bestParams.put(SecondOrderRMRunner.TARGET_RM_WEIGHT, targetRMWeight);
					bestParams.put(SecondOrderRMRunner.EXTERNAL_RM_WEIGHT, wikiRMWeight);
				}
			}
		}
		
		return bestParams;
	}
}