package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.entities.docscoring.expansion.ExpansionRM3Builder;
import edu.gslis.entities.docscoring.expansion.SingleExpansionRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityRMRunner extends QueryRunner {
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private QueryScorer docScorerQueryProb;
	private DocScorer expansionScorer;
	private QueryScorer expansionScorerQueryProb;
	private RelatedDocs clusters;
	private IndexWrapper expansionIndex;
		
	public EntityRMRunner(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DocScorer docScorer, QueryScorer docScorerQueryProb, DocScorer expansionScorer, QueryScorer expansionScorerQueryProb,
			RelatedDocs clusters, IndexWrapper expansionIndex) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorer = docScorer;
		this.docScorerQueryProb = docScorerQueryProb;
		this.expansionScorer = expansionScorer;
		this.expansionScorerQueryProb = expansionScorerQueryProb;
		this.clusters = clusters;
		this.expansionIndex = expansionIndex;
	}

	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();

		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(EntityRunner.DOCUMENT_WEIGHT, origWeight);

			double expansionWeight = (10 - origW) / 10.0;
			currentParams.put(EntityRunner.EXPANSION_WEIGHT, expansionWeight);
				
			for (int lambda = 0; lambda <= 10; lambda += 1) {
				double lam = lambda / 10.0;
				currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, lam);

				System.err.println("\t\tParameters: "+origWeight+" (doc), "+expansionWeight+" (expansion), "+lam+" (mixing)");
				SearchHitsBatch batchResults = run(queries, NUM_TRAINING_RESULTS, currentParams);

				double metricVal = evaluator.evaluate(batchResults);
				
				System.err.println("\t\tScore: "+metricVal);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.putAll(currentParams);
				}
			}
		}
		
		System.err.println("Best parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println(param+": "+bestParams.get(param));
		}
		return bestParams;
	}

	@Override
	public SearchHits runQuery(QueryParameters queryParams) {
		GQuery query = queryParams.getQuery();
		int numResults = queryParams.getNumResults();
		Map<String, Double> params = queryParams.getParams();
		
		query.applyStopper(stopper);
		
		int fbDocs = 20;
		if (params.containsKey(RMRunner.FEEDBACK_DOCUMENTS)) {
			fbDocs = params.get(RMRunner.FEEDBACK_DOCUMENTS).intValue();
		}
		int fbTerms = 20;
		if (params.containsKey(RMRunner.FEEDBACK_TERMS)) {
			params.get(RMRunner.FEEDBACK_TERMS).intValue();
		}

		SearchHits initialHits = getInitialHits(query);
		
		SingleExpansionRM1Builder rm1 = new SingleExpansionRM1Builder(query,
				initialHits, docScorer, docScorerQueryProb,
				expansionScorer, expansionScorerQueryProb,
				clusters, expansionIndex,
				fbDocs, fbTerms);
		ExpansionRM3Builder rm3 = new ExpansionRM3Builder(query, rm1);
		FeatureVector rm3Vector = rm3.buildRelevanceModel(stopper, params);
		
		System.err.println("RM3 for query "+query.getTitle()+" ("+query.getText()+"):");
		System.err.println(rm3Vector.toString(10));

		FeatureVector origQueryVec = query.getFeatureVector();
		query.setFeatureVector(rm3Vector);
		
		// Run the new query against the target index
		SearchHits results = targetIndex.runQuery(query, numResults);
		
		query.setFeatureVector(origQueryVec);
		
		return results;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
