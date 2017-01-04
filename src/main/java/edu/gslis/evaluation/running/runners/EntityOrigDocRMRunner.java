package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.entities.docscoring.expansion.FileLookupRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RM3Builder;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.retrieval.QueryResults;

/**
 * Builds RM3 on the original document, which it uses to search with expansion.
 * @author Garrick
 *
 */
public class EntityOrigDocRMRunner extends QueryRunner {
	
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private DocScorer expansionScorer;
	private String rmDir;
		
	public EntityOrigDocRMRunner(SearchHitsBatch initialResultsBatch, Stopper stopper, String rmDir,
			DocScorer docScorer, DocScorer expansionScorer) {
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.rmDir = rmDir;
		this.docScorer = docScorer;
		this.expansionScorer = expansionScorer;
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

		System.err.println("Computing query " + query.getTitle() + " fresh");
		
		query.applyStopper(stopper);
		
		SearchHits initialHits = getInitialHits(query);
		
		QueryResults queryResults = new QueryResults(query, initialHits);

		RM1Builder rm1 = new FileLookupRM1Builder(rmDir);
		RM3Builder rm3 = new RM3Builder();
		FeatureVector rm3Vector = rm3.buildRelevanceModel(queryResults, rm1,
				params.get(RMRunner.ORIG_QUERY_WEIGHT));
		rm3Vector.clip(20);
		
		//System.err.println("RM3 for query " + query.getTitle() + " (" +
		//		query.getText() + "):");
		//System.err.println(rm3Vector.toString(10));

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
			
			Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
			scorerWeights.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT));
			scorerWeights.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT));
			DocScorer interpolatedScorer = new InterpolatedDocScorer(scorerWeights);
			
			QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);

			SearchHit newDoc = new SearchHit();
			newDoc.setDocno(doc.getDocno());	
			newDoc.setScore(queryScorer.scoreQuery(newQuery, doc));

			processedHits.add(newDoc);
		}
		
		processedHits.rank();
		return processedHits;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
