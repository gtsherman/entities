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

public class EntityRunner implements QueryRunner {
	
	public static final String DOCUMENT_WEIGHT = "document";
	public static final String EXPANSION_WEIGHT = "wiki";
	public static final String WIKI_MODEL = "Wiki";
	public static final String SELF_MODEL = "Self";

	private SearchHitsBatch initialResultsBatch;
	private Stopper stopper;
	private DocScorer docScorer;
	private DocScorer expansionDocScorer;

	private ParameterizedResults processedQueries = new ParameterizedResults();
	
	public EntityRunner(SearchHitsBatch initialResultsBatch, Stopper stopper,
			DocScorer docScorer, DocScorer expansionDocScorer) {
		this.initialResultsBatch = initialResultsBatch;
		this.stopper = stopper;
		this.docScorer = docScorer;
		this.expansionDocScorer = expansionDocScorer;
	}
	
	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			currentParams.put(EntityRunner.DOCUMENT_WEIGHT, origWeight);
			
			double wikiWeight = (10 - origW) / 10.0;
			currentParams.put(EntityRunner.EXPANSION_WEIGHT, wikiWeight);
				
			System.err.println("\t\tParameters: " + origWeight + " (doc), " + wikiWeight + " (expansion)");
			SearchHitsBatch batchResults = run(queries, 100, currentParams);
			
			double metricVal = evaluator.evaluate(batchResults);
			if (metricVal > maxMetric) {
				maxMetric = metricVal;
				bestParams.putAll(currentParams);
			}
		}
		
		System.err.println("\tBest parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println("\t\t" + param + ": " + bestParams.get(param));
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
			batchResults.setSearchHits(query, processedHits);
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQueryResults(GQuery query, int numResults, Map<String, Double> params) {
		double[] paramVals = {params.get(DOCUMENT_WEIGHT), params.get(EXPANSION_WEIGHT), numResults};

		if (!processedQueries.resultsExist(query, paramVals)) {
			query.applyStopper(stopper);
			
			SearchHits initialHits = getInitialHits(query);
			SearchHits processedHits = new SearchHits();

			int i = 0;
			Iterator<SearchHit> hitIt = initialHits.iterator();
			while (hitIt.hasNext() && i < numResults) {
				SearchHit doc = hitIt.next();
				i++;
				
				SearchHit newDoc = new SearchHit();
				newDoc.setDocno(doc.getDocno());
				newDoc.setQueryName(query.getTitle());

				Map<DocScorer, Double> scorerWeights = new HashMap<DocScorer, Double>();
				scorerWeights.put(docScorer, params.get(DOCUMENT_WEIGHT));
				scorerWeights.put(expansionDocScorer, params.get(EXPANSION_WEIGHT));
				
				DocScorer interpolatedScorer = new InterpolatedDocScorer(scorerWeights);
				
				QueryScorer queryScorer = new QueryLikelihoodQueryScorer(interpolatedScorer);
				
				newDoc.setScore(queryScorer.scoreQuery(query, doc));
				processedHits.add(newDoc);
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
