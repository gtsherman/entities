package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.entities.docscoring.expansion.ExpansionRM3Builder;
import edu.gslis.entities.docscoring.expansion.MultiExpansionRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class DoubleEntityRMRunner extends QueryRunner {
	
	private final int trainingNumResults = 100;
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DirichletDocScorer docScorer;
	private ExpansionDocsDocScorer selfExpansionScorer;
	private ExpansionDocsDocScorer wikiExpansionScorer;
	
	public DoubleEntityRMRunner(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DirichletDocScorer docScorer, ExpansionDocsDocScorer selfExpansionScorer,
			ExpansionDocsDocScorer wikiExpansionScorer) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorer = docScorer;
		this.selfExpansionScorer = selfExpansionScorer;
		this.wikiExpansionScorer = wikiExpansionScorer;
	}

	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();

		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(DoubleEntityRunner.DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;
				
				currentParams.put(DoubleEntityRunner.WIKI_WEIGHT, wikiWeight);
				currentParams.put(DoubleEntityRunner.SELF_WEIGHT, selfWeight);
				
				for (int lambda = 0; lambda <= 10; lambda += 1) {
					double lam = lambda / 10.0;
					currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, lam);

					System.err.println("\t\tParameters: "+origWeight+" (doc), "+wikiWeight+" (wiki), "+selfWeight+" (self), "+lam+" (mixing)");
					SearchHitsBatch batchResults = run(queries, trainingNumResults, currentParams);

					double metricVal = evaluator.evaluate(batchResults);

					System.err.println("\t\tScore: "+metricVal);
					if (metricVal > maxMetric) {
						maxMetric = metricVal;
						bestParams.putAll(currentParams);
					}
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
		
		// Setup RM1 and RM3 builders
		MultiExpansionRM1Builder rm1Builder = new MultiExpansionRM1Builder(query, initialHits,
				docScorer, selfExpansionScorer, wikiExpansionScorer,
				fbDocs, fbTerms);
		ExpansionRM3Builder rm3Builder = new ExpansionRM3Builder(query, rm1Builder);
		
		// Build the RM3 and convert to query
		FeatureVector rm3Vector = rm3Builder.buildRelevanceModel(stopper, params);
		
		System.err.println("RM3 for query "+query.getTitle()+" ("+query.getText()+"):");
		System.err.println(rm3Vector.toString(10));
		
		GQuery newQuery = new GQuery();
		newQuery.setTitle(query.getTitle());
		newQuery.setFeatureVector(rm3Vector);
		
		// Run the new query against the target index
		SearchHits results = targetIndex.runQuery(newQuery, numResults);
		
		return results;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
