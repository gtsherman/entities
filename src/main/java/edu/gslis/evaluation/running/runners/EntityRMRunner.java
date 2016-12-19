package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.keyvalue.MultiKey;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.entities.docscoring.ExpansionDocsDocScorer;
import edu.gslis.entities.docscoring.expansion.ExpansionRM3Builder;
import edu.gslis.entities.docscoring.expansion.SingleExpansionRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityRMRunner implements QueryRunner {
	
	private final int trainingNumResults = 100;
	private final int QUERY_KEY_INDEX = 0;
	private final int NUMRESULTS_KEY_INDEX = 1;
	private final int PARAMS_KEY_INDEX = 2;
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DirichletDocScorer docScorer;
	private ExpansionDocsDocScorer expansionScorer;
		
	private LoadingCache<MultiKey, SearchHits> processedQueries = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<MultiKey, SearchHits>() {
						public SearchHits load(MultiKey key) throws Exception {
							GQuery query = (GQuery) key.getKey(QUERY_KEY_INDEX);
							int numResults = (Integer) key.getKey(NUMRESULTS_KEY_INDEX);
							@SuppressWarnings("unchecked")
							Map<String, Double> params = (Map<String, Double>) key.getKey(PARAMS_KEY_INDEX);
							
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
							
							SingleExpansionRM1Builder rm1 = new SingleExpansionRM1Builder(query, initialHits, docScorer, expansionScorer, fbDocs, fbTerms);
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
					});
	
	public EntityRMRunner(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DirichletDocScorer docScorer, ExpansionDocsDocScorer expansionScorer) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
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
				SearchHitsBatch batchResults = run(queries, trainingNumResults, currentParams);

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
		Object[] keys = new Object[3];
		keys[QUERY_KEY_INDEX] = query;
		keys[NUMRESULTS_KEY_INDEX] = numResults;
		keys[PARAMS_KEY_INDEX] = params;
		
		MultiKey key = new MultiKey(keys);
		
		// Lookup in cache
		try {
			return processedQueries.get(key);
		} catch (ExecutionException e) {
			System.err.println("Error scoring query " + query.getTitle());
			System.err.println(e.getStackTrace());
		}
		
		// Default to zero, if we have an issue
		return new SearchHits();
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
