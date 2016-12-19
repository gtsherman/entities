package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.ParameterizedResults;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.InterpolatedDocScorer;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;

public class DoubleEntityRunner implements QueryRunner {
	
	public static final String DOCUMENT_WEIGHT = "document";
	public static final String WIKI_WEIGHT = "wiki";
	public static final String SELF_WEIGHT = "self";

	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private DocScorer selfExpansionScorer;
	private DocScorer wikiExpansionScorer;
	
	private ParameterizedResults processedQueries = new ParameterizedResults();
	
	public DoubleEntityRunner(SearchHitsBatch initialResultsBatch, Stopper stopper,
			DocScorer docScorer, DocScorer selfExpansionScorer,
			DocScorer wikiExpansionScorer) {
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
			currentParams.put(DOCUMENT_WEIGHT, origWeight);
			for (int wikiW = 0; wikiW <= 10-origW; wikiW++) {
				double wikiWeight = wikiW / 10.0;
				double selfWeight = (10-(origW+wikiW)) / 10.0;
				
				currentParams.put(WIKI_WEIGHT, wikiWeight);
				currentParams.put(SELF_WEIGHT, selfWeight);

				System.err.println("\t\tParameters: " + origWeight + " (doc), " + wikiWeight + " (wiki), " + selfWeight + " (self)");
				SearchHitsBatch batchResults = run(queries, 100, currentParams);
				
				double metricVal = evaluator.evaluate(batchResults);
				if (metricVal > maxMetric) {
					maxMetric = metricVal;
					bestParams.putAll(currentParams);
				}
			}
		}
	
		System.err.println("\tBest parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println("\t\t" + param + ": "+bestParams.get(param));
		}
		return bestParams;
	}

	@Override
	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			SearchHits processedHits = getProcessedQueryResults(query, numResults, params);			
			batchResults.setSearchHits(query.getTitle(), processedHits);
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQueryResults(GQuery query, int numResults, Map<String, Double> params) {
		double[] paramVals = {params.get(DOCUMENT_WEIGHT),
				params.get(SELF_WEIGHT),
				params.get(WIKI_WEIGHT),
				numResults};

		if (!processedQueries.resultsExist(query, paramVals)) {
			query.applyStopper(stopper);
			
			SearchHits initialHits = getInitialHits(query);
			SearchHits processedHits = new SearchHits();

			int i = 0;
			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext() && i < numResults) {
				SearchHit doc = hitIt.next();
				i++;
				
				SearchHit newHit = new SearchHit();
				newHit.setDocno(doc.getDocno());
				newHit.setQueryName(query.getTitle());

				Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
				scorerWeights.put(docScorer, params.get(DOCUMENT_WEIGHT));
				scorerWeights.put(wikiExpansionScorer, params.get(WIKI_WEIGHT));
				scorerWeights.put(selfExpansionScorer, params.get(SELF_WEIGHT));
				
				DocScorer interpolatedScorer = new InterpolatedDocScorer(scorerWeights);
				
				QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
			
				newHit.setScore(queryScorer.scoreQuery(query, doc));
				processedHits.add(newHit);
			}
			
			processedHits.rank();
			processedQueries.addResults(processedHits, query, paramVals);
		}

		return processedQueries.getResults(query, paramVals);
	}
	
	private SearchHits getInitialHits(GQuery query) {
		return initialResultsBatch.getSearchHits(query);
	}

}
