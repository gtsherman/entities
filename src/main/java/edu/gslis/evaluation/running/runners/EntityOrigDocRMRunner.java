package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.entities.docscoring.creators.ExpansionDocsDocScorerCreator;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.ParameterizedResults;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.creators.DirichletDocScorerCreator;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RM3Builder;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

/**
 * Builds RM3 on the original document, which it uses to search with expansion.
 * @author Garrick
 *
 */
public class EntityOrigDocRMRunner implements QueryRunner {
	
	private final int trainingNumResults = 100;
	
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DirichletDocScorerCreator docScorerCreator;
	private ExpansionDocsDocScorerCreator expansionScorerCreator;
		
	private ParameterizedResults processedQueries = new ParameterizedResults();
	
	public EntityOrigDocRMRunner(SearchHitsBatch initialResultsBatch, Stopper stopper,
			DirichletDocScorerCreator docScorerCreator, ExpansionDocsDocScorerCreator expansionScorerCreator) {
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorerCreator = docScorerCreator;
		this.expansionScorerCreator = expansionScorerCreator;
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

			CollectionStats collectionStats = docScorerCreator.getCollectionStats();
			RM1Builder rm1 = new RM1Builder(query, initialHits, fbDocs, fbTerms, collectionStats);
			RM3Builder rm3 = new RM3Builder(query, rm1);
			FeatureVector rm3Vector = rm3.buildRelevanceModel(params.get(RMRunner.ORIG_QUERY_WEIGHT), stopper);
			
			System.err.println("RM3 for query "+query.getTitle()+" ("+query.getText()+"):");
			System.err.println(rm3Vector.toString(10));

			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3Vector);
			
			// Run the new query against the target index
			SearchHits processedHits = new SearchHits();
			int i = 0;
			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext() && i < numResults) {
				SearchHit doc = hitIt.next();
				i++;
				
				SearchHit newDoc = new SearchHit();
				newDoc.setDocno(doc.getDocno());
				newDoc.setFeatureVector(doc.getFeatureVector());
				newDoc.setQueryName(query.getTitle());

				DocScorer docScorer = docScorerCreator.getDocScorer(newDoc);
				DocScorer expansionDocScorer = expansionScorerCreator.getDocScorer(newDoc);

				Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
				scorerWeights.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT));
				scorerWeights.put(expansionDocScorer, params.get(EntityRunner.EXPANSION_WEIGHT));
				DocScorer interpolatedScorer = new InterpolatedDocScorer(scorerWeights);
				
				QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
				
				newDoc.setScore(queryScorer.scoreQuery(query));
				processedHits.add(newDoc);
			}
			
			processedHits.rank();
			
			// Add the results to our processed queries map
			processedQueries.addResults(processedHits, query, paramVals);
		}

		return processedQueries.getResults(query, paramVals);
	}
	
	private double[] getParamVals(Map<String, Double> params, int numResults) {
		double[] paramsVals = {params.get(EntityRunner.DOCUMENT_WEIGHT),
			params.get(EntityRunner.EXPANSION_WEIGHT),
			params.get(RMRunner.ORIG_QUERY_WEIGHT),
			numResults};
		return paramsVals;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
