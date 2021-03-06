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

public class DoubleEntityRMRunner extends QueryRunner {
	
	private final int trainingNumResults = 100;
	
	private IndexWrapper targetIndex;
	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private DocScorer docScorerQueryProb;
	private DocScorer selfScorer;
	private DocScorer selfScorerQueryProb;
	private DocScorer wikiScorer;
	private DocScorer wikiScorerQueryProb;
	private TermCollector termCollector;
	
	private LoadingCache<QueryParameters, FeatureVector> rm1s = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<QueryParameters, FeatureVector>() {
						public FeatureVector load(QueryParameters key) throws Exception {
							return computeRM1(key);
						}
					});
	
	public DoubleEntityRMRunner(IndexWrapper targetIndex, SearchHitsBatch initialResultsBatch, Stopper stopper,
			DocScorer docScorer, DocScorer selfExpansionScorer,
			DocScorer wikiExpansionScorer, DocScorer docScorerQuery,
			DocScorer selfExpansionScorerQuery, DocScorer wikiExpansionScorerQuery,
			TermCollector termCollector) {
		this.targetIndex = targetIndex;
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorer = docScorer;
		this.selfScorer = selfExpansionScorer;
		this.wikiScorer = wikiExpansionScorer;
		this.docScorerQueryProb = docScorerQuery;
		this.selfScorerQueryProb = selfExpansionScorerQuery;
		this.wikiScorerQueryProb = wikiExpansionScorerQuery;
		this.termCollector = termCollector;
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
		
		// Build the RM3 and convert to query
		RM1Builder rm1Builder;
		try {
			rm1Builder = new StoredRM1Builder(rm1s.get(queryParams));
		} catch (ExecutionException e) {
			e.printStackTrace(System.err);
			rm1Builder = new StoredRM1Builder(computeRM1(queryParams));
		}		
		RM3Builder rm3Builder = new RM3Builder();
		FeatureVector rm3Vector = rm3Builder.buildRelevanceModel(query,
				getInitialHits(query), rm1Builder,
				params.get(RMRunner.ORIG_QUERY_WEIGHT));
		
		System.err.println("RM3 for query "+query.getTitle()+" ("+query.getText()+"):");
		System.err.println(rm3Vector.toString(10));
		
		GQuery newQuery = new GQuery();
		newQuery.setTitle(query.getTitle());
		newQuery.setFeatureVector(rm3Vector);
		
		// Run the new query against the target index
		SearchHits results = targetIndex.runQuery(newQuery, numResults);
		
		return results;
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
		queryScorers.put(docScorerQueryProb, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
		queryScorers.put(selfScorerQueryProb, params.get(DoubleEntityRunner.SELF_WEIGHT));
		queryScorers.put(wikiScorerQueryProb, params.get(DoubleEntityRunner.WIKI_WEIGHT));

		Map<DocScorer, Double> docScorers = new HashMap<DocScorer, Double>();
		docScorers.put(docScorer, params.get(DoubleEntityRunner.DOCUMENT_WEIGHT));
		docScorers.put(selfScorer, params.get(DoubleEntityRunner.SELF_WEIGHT));
		docScorers.put(wikiScorer, params.get(DoubleEntityRunner.WIKI_WEIGHT));
		
		// Setup RM1 builder and build
		IndeterminateExpansionRM1Builder rm1Builder = new IndeterminateExpansionRM1Builder(query, initialHits,
				docScorers, queryScorers, termCollector, fbDocs, fbTerms);
		return rm1Builder.buildRelevanceModel(query, initialHits, stopper);
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
