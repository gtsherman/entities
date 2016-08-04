package edu.gslis.evaluation.validators;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;

public interface Validator {

	/**
	 * Set ground truth judgments.
	 * @param qrels Qrels representing ground truth
	 */
	public void setQrels(Qrels qrels);

	/**
	 * Set full query set.
	 * @param queries Queries to use for training
	 */
	public void setQueries(GQueries queries);
	
	/**
	 * Evaluate the model.
	 * @return The SearchHitsBatch representing one unified run
	 */
	public SearchHitsBatch evaluate(Evaluator evaluator);

}
