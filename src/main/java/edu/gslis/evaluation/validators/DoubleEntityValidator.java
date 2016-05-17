package edu.gslis.evaluation.validators;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;
import edu.gslis.queryrunning.DoubleEntityRunner;
import edu.gslis.queryrunning.QueryRunner;

public class DoubleEntityValidator extends KFoldValidator {

	public DoubleEntityValidator(QueryRunner runner) {
		super(runner);
	}
	
	public DoubleEntityValidator(QueryRunner runner, int k) {
		super(runner, k);
	}

	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		runner.setQueries(queries);
		
		double maxMetric = 0.0;
		Map<String, Double> bestParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			runner.setParameter(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;
				
				runner.setParameter(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
				runner.setParameter(DoubleEntityRunner.SELF_WEIGHT, selfWeight);

				runner.run(100);
				
				double metricVal = evaluator.evaluate(runner.getBatchResults(), qrels);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
					bestParams.put(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
					bestParams.put(DoubleEntityRunner.SELF_WEIGHT, selfWeight);
				}
			}
		}
		
		return bestParams;
	}

}
