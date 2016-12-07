package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.entities.docscoring.expansion.ExpansionRM3Builder;
import edu.gslis.entities.docscoring.expansion.MultiExpansionRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.ParameterizedResults;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.creators.DirichletDocScorerCreator;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class DoubleEntityRMRunner implements QueryRunner {
	
	private final int trainingNumResults = 100;
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DirichletDocScorerCreator docScorerCreator;
	private ExpansionDocsDocScorerCreator selfExpansionScorerCreator;
	private ExpansionDocsDocScorerCreator wikiExpansionScorerCreator;
		
	private ParameterizedResults processedQueries = new ParameterizedResults();
	
	public DoubleEntityRMRunner(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DirichletDocScorerCreator docScorerCreator, ExpansionDocsDocScorerCreator selfExpansionScorerCreator,
			ExpansionDocsDocScorerCreator wikiExpansionScorerCreator) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorerCreator = docScorerCreator;
		this.selfExpansionScorerCreator = selfExpansionScorerCreator;
		this.wikiExpansionScorerCreator = wikiExpansionScorerCreator;
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

					//double metricVal = evaluator.evaluate(batchResults);
					double metricVal = 0.0;
					
					Iterator<String> queryIt = batchResults.queryIterator();
					while (queryIt.hasNext()) {
						String query = queryIt.next();
						GQuery gquery = new GQuery();
						gquery.setTitle(query);

						if (processedQueries.scoreExists(gquery, getParamVals(currentParams, trainingNumResults))) {
							metricVal += processedQueries.getScore(gquery, getParamVals(currentParams, trainingNumResults));
						} else {
							SearchHitsBatch q = new SearchHitsBatch();
							q.setSearchHits(gquery, batchResults.getSearchHits(query));

							double score = evaluator.evaluate(q);
							processedQueries.setScore(score, gquery, getParamVals(currentParams, trainingNumResults));

							metricVal += score;
						}
					}
					metricVal /= batchResults.getNumQueries();
					
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
	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		
		Iterator<GQuery> qIt = queries.iterator();
		while (qIt.hasNext()) {
			GQuery query = qIt.next();
			
			SearchHits results = getProcessedQuery(query, numResults, params);
			batchResults.setSearchHits(query.getTitle(), results);	
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQuery(GQuery query, int numResults, Map<String, Double> params) {
		double[] paramVals = getParamVals(params, numResults);

		if (!processedQueries.resultsExist(query, paramVals)) {
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
					docScorerCreator, selfExpansionScorerCreator, wikiExpansionScorerCreator,
					fbDocs, fbTerms);
			ExpansionRM3Builder rm3Builder = new ExpansionRM3Builder(query, rm1Builder);
			
			// Build the RM3 and convert to query
			FeatureVector rm3Vector = rm3Builder.buildRelevanceModel(stopper, params);
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3Vector);
			
			// Run the new query against the target index
			SearchHits results = targetIndex.runQuery(newQuery, numResults);
			
			// Add the results to our processed queries map
			processedQueries.addResults(results, query, paramVals);
		}

		return processedQueries.getResults(query, paramVals);
	}
	
	private double[] getParamVals(Map<String, Double> params, int numResults) {
		double[] paramsVals = {params.get(DoubleEntityRunner.DOCUMENT_WEIGHT),
			params.get(DoubleEntityRunner.SELF_WEIGHT),
			params.get(DoubleEntityRunner.WIKI_WEIGHT),
			params.get(RMRunner.ORIG_QUERY_WEIGHT),
			numResults};
		return paramsVals;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
