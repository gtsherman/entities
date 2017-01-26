package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.entities.docscoring.expansion.IndeterminateExpansionRM1Builder;
import edu.gslis.entities.docscoring.expansion.StoredRM1Builder;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.related_docs.term_collectors.TermCollector;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RM3Builder;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class EntityRMRunnerExpansionScoreExpanded extends QueryRunner {
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private DocScorer docScorerQueryProb;
	private DocScorer expansionScorer;
	private DocScorer expansionScorerQueryProb;
	private TermCollector termCollector;
	
	private EntityRunner rescorer;
	
	private LoadingCache<QueryParameters, FeatureVector> rm1s = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<QueryParameters, FeatureVector>() {
						public FeatureVector load(QueryParameters key) throws Exception {
							return computeRM1(key);
						}
					});
		
	public EntityRMRunnerExpansionScoreExpanded(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DocScorer docScorer, DocScorer docScorerQueryProb, DocScorer expansionScorer, DocScorer expansionScorerQueryProb,
			TermCollector termCollector) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorer = docScorer;
		this.docScorerQueryProb = docScorerQueryProb;
		this.expansionScorer = expansionScorer;
		this.expansionScorerQueryProb = expansionScorerQueryProb;
		this.termCollector = termCollector;

		rescorer = new EntityRunner(initialResultsBatch, stopper,
				docScorerQueryProb, expansionScorerQueryProb);
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
		
		// A subset of parameters that matters to building an RM1:
		Map<String, Double> paramsSubsetForRM1 = new HashMap<String, Double>(params);
		paramsSubsetForRM1.remove(RMRunner.ORIG_QUERY_WEIGHT);
		QueryParameters rm1Params = new QueryParameters(query, numResults,
				paramsSubsetForRM1); 
		
		// Build the RM3 and convert to query
		RM1Builder rm1Builder;
		try {
			rm1Builder = new StoredRM1Builder(rm1s.get(rm1Params));
		} catch (ExecutionException e) {
			e.printStackTrace(System.err);
			rm1Builder = new StoredRM1Builder(computeRM1(rm1Params));
		}		
		RM3Builder rm3Builder = new RM3Builder();
		FeatureVector rm3Vector = rm3Builder.buildRelevanceModel(query,
				getInitialHits(query), rm1Builder,
				params.get(RMRunner.ORIG_QUERY_WEIGHT));
		
		FeatureVector origQueryVec = query.getFeatureVector();
		query.setFeatureVector(rm3Vector);
		
		// Rescore the new results with expansion
		SearchHits results = rescorer.runQuery(queryParams);
		
		query.setFeatureVector(origQueryVec);
		
		return results;
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}
	
	private FeatureVector computeRM1(QueryParameters queryParams) {
		GQuery query = queryParams.getQuery();
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
		
		Map<DocScorer, Double> queryScorers = new HashMap<DocScorer, Double>();
		queryScorers.put(docScorerQueryProb, params.get(EntityRunner.DOCUMENT_WEIGHT));
		queryScorers.put(expansionScorerQueryProb, params.get(EntityRunner.EXPANSION_WEIGHT));

		Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
		docScorers.put(docScorer, params.get(EntityRunner.DOCUMENT_WEIGHT));
		docScorers.put(expansionScorer, params.get(EntityRunner.EXPANSION_WEIGHT));
		
		IndeterminateExpansionRM1Builder rm1 = new IndeterminateExpansionRM1Builder(query,
				initialHits, docScorers, queryScorers, termCollector,
				fbDocs, fbTerms);
		return rm1.buildRelevanceModel(query, initialHits);
	}

}
